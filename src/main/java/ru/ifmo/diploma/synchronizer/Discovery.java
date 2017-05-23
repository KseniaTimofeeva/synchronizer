package ru.ifmo.diploma.synchronizer;

import ru.ifmo.diploma.synchronizer.protocol.Credentials;
import ru.ifmo.diploma.synchronizer.protocol.MagicPackage;
import ru.ifmo.diploma.synchronizer.protocol.YesNoPackage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ksenia on 21.05.2017.
 */
public class Discovery {

    private int port;
    private Map<String, Socket> connections = new ConcurrentHashMap<>();
    private Set<String> currentHostAddresses;

    //@TODO карта маршрутизации хранится на диске

    private Map<String, Credentials> authorizationTable = new HashMap<>();

    public Discovery(int port) {
        this.port = port;
    }

    public Discovery(int port, Map<String, Credentials> authorizationTable) {
        this.port = port;
        this.authorizationTable = authorizationTable;
    }

    public void startDiscovery() {

        new AcceptThread().start(); //стартуем поток для входящих соединений

        currentHostAddresses = currentHostAddresses();

        for (Map.Entry<String, Credentials> entry : authorizationTable.entrySet()) {
            String addr = entry.getKey();

            if (checkIsCurrentHost(addr)) {
                continue;
            }

            Socket socket;
            synchronized (connections) {
                if (checkIPAlreadyExist(addr)) { //если подключение с этим адресом есть
                    continue;
                }
                //если такого подключения нет, то подключаемся
                socket = new Socket();
                connections.put(addr, socket);
            }

            try {
                socket.connect(parseAddress(addr));
                new OutputConnectionThread(addr, socket).start();

            } catch (IOException e) {
                closeSocket(socket);
                connections.remove(addr);
                System.err.println(port + ": Error connecting to " + addr);
            }
        }
    }

    private Set<String> currentHostAddresses() {
        currentHostAddresses = new HashSet<>();
        //получаем список доступных адресов на текущем хосте
        Enumeration<NetworkInterface> n = null;
        try {
            n = NetworkInterface.getNetworkInterfaces();

            while (n.hasMoreElements()) {
                NetworkInterface e = n.nextElement();
                Enumeration<InetAddress> a = e.getInetAddresses();
                while (a.hasMoreElements()) {
                    InetAddress inetAddress = a.nextElement();
                    currentHostAddresses.add(inetAddress.getHostAddress() + ":" + port);
                }
            }
        } catch (SocketException e) {
            //@TODO
            e.printStackTrace();
        }
        return currentHostAddresses;
    }

    private boolean checkIsCurrentHost(String addr) {
        return currentHostAddresses.contains(addr);
    }


    private boolean checkIPAlreadyExist(String addr) {
        return connections.get(addr) != null;
    }


    private class AcceptThread extends Thread {
        @Override
        public void run() {

            while (!isInterrupted()) {

                try (ServerSocket ssocket = new ServerSocket(port)) {   //стартуем на заданном порту
                    System.out.println(port + ": Server started on " + ssocket);

                    while (true) {
                        Socket socket = ssocket.accept();

                        String addr = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();

                        synchronized (connections) {
                            if (checkIPAlreadyExist(addr)) { //если данный хост уже подключен
                                closeSocket(socket);
                                continue;
                            }
                            connections.put(addr, socket);
                        }
                        new InputConnectionThread(addr, socket).start();
                    }
                } catch (IOException e) {
                    System.err.println(port + ": Server error");
                    e.printStackTrace();
                }
            }
        }
    }

    private void closeSocket(Socket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private class OutputConnectionThread extends Thread {
        private String addr;
        private Socket socket;

        private OutputConnectionThread(String addr, Socket socket) {
            super();
            this.addr = addr;
            this.socket = socket;
        }

        @Override
        public void run() {
            System.out.println(port + ": " + socket + " connected to " + addr);

            try (ObjectOutputStream objOut = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream objIn = new ObjectInputStream((socket.getInputStream()))) {

                while (!isInterrupted()) {

                    objOut.writeObject("hello from " + port);
                    objOut.flush();

                    objOut.writeObject(new MagicPackage(port));
                    objOut.flush();

                    objOut.writeObject(new Credentials("login" + port, "password" + port));
                    objOut.flush();

                    while (true) {
                        try {
                            Object obj = objIn.readObject();

                            if (obj instanceof YesNoPackage) {

                            }

                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    }

                }

            } catch (IOException e) {
                closeSocket(socket);
                connections.remove(addr);
                System.err.println(port + ": " + socket + " disconnected");
                e.printStackTrace();
                interrupt();
            }
        }
    }


    private class InputConnectionThread extends Thread {
        private String addr;
        private Socket socket;

        private InputConnectionThread(String addr, Socket socket) {
            super();
            this.addr = addr;
            this.socket = socket;
        }

        @Override
        public void run() {

            try (ObjectInputStream objIn = new ObjectInputStream(socket.getInputStream());
                 ObjectOutputStream objOut = new ObjectOutputStream(socket.getOutputStream())) {

                while (!isInterrupted()) {

                    Object obj = objIn.readObject();

                    if (obj instanceof String) {
                        System.out.println(obj + " to " + port);
                    } else if (obj instanceof MagicPackage) {
                        System.out.println(obj + " from " + ((MagicPackage) obj).getFrom() + " to " + port);

                        continue;
                    } else if (obj instanceof Credentials) {
                        System.out.println(obj + " to " + port);

                        //@TODO check credentials


                        objOut.writeObject(new YesNoPackage(true));
                    } else {
                        closeSocket(socket);
                        connections.remove(addr);
                    }
                }

            } catch (IOException e) {
                closeSocket(socket);
                connections.remove(addr);
//                    System.err.println(port + ": " + socket + " disconnected ");
                e.printStackTrace();
                interrupt();
            } catch (ClassNotFoundException e) {
                System.err.println(port + ": Data transferring error");
                e.printStackTrace();
            }
        }

    }

    private SocketAddress parseAddress(String addr) {
        String[] split = addr.split(":");
        return new InetSocketAddress(split[0], Integer.parseInt(split[1]));
    }
}
