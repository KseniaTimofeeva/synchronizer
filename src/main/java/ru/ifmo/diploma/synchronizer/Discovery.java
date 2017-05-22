package ru.ifmo.diploma.synchronizer;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ksenia on 21.05.2017.
 */
public class Discovery {

    private Map<String, Socket> connections = new ConcurrentHashMap<>();

    private Map<String, Credentials> authorizationTable = new HashMap<>();

    public void start() {

        new Thread(new OutputThread()).start(); //стартуем поток для прохода по карте маршрутизации

        try (ServerSocket ssocket = new ServerSocket(12345)) {
            System.out.println("App started on " + ssocket);

            while (true) {
                Socket socket = ssocket.accept();

                String addr = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();

                synchronized (connections) {
                    if (checkIPAlreadyExist(addr)) { //если данный хост уже подключен
                        socket.close();
                        continue;
                    }
                    connections.put(addr, socket);
                }
                new Thread(new InputThread(socket)).start(); //если нет, то создаем для него новый поток

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean checkIPAlreadyExist(String addr) {
        Socket availableSocket = connections.get(addr);
        if (availableSocket != null) {
            return true;
        }
        return false;
    }

    private class OutputThread implements Runnable {
        @Override
        public void run() {
            for (Map.Entry<String, Credentials> entry : authorizationTable.entrySet()) {
                String addr = entry.getKey();

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
                    try (ObjectOutputStream objOut = new ObjectOutputStream(socket.getOutputStream());
                         ObjectInputStream objIn = new ObjectInputStream((socket.getInputStream()))) {


                    }
                } catch (IOException e) {
                    //@TODO
                    e.printStackTrace();
                }
            }
        }
    }


    private class InputThread implements Runnable {
        private Socket socket;

        private InputThread(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (ObjectInputStream objIn = new ObjectInputStream(socket.getInputStream());
                 ObjectOutputStream objOut = new ObjectOutputStream(socket.getOutputStream())) {

                System.out.println(socket.getInetAddress().getHostAddress() + " connected");
                while (!Thread.currentThread().isInterrupted()) {

                    Object obj = objIn.readObject();

                    //@TODO

                    if (obj instanceof MagicPackage) {
                    } else {
                    }
                }
            } catch (IOException e) {
                System.err.println(socket.getInetAddress().getHostAddress() + "disconnected ");
            } catch (ClassNotFoundException e) {
                //@TODO
                e.printStackTrace();
            }
        }
    }

    private SocketAddress parseAddress(String addr) {
        String[] split = addr.split(":");
        return new InetSocketAddress(split[0], Integer.parseInt(split[1]));
    }

    private static class Credentials {
        private String login;
        private String password;

        public Credentials(String login, String password) {
            this.login = login;
            this.password = password;
        }
    }
}
