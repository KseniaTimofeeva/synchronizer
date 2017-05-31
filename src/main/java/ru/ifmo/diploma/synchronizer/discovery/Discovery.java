package ru.ifmo.diploma.synchronizer.discovery;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.ifmo.diploma.synchronizer.Utils;
import ru.ifmo.diploma.synchronizer.exchange.TReader;
import ru.ifmo.diploma.synchronizer.exchange.TWriter;
import ru.ifmo.diploma.synchronizer.protocol.exchange.AbstractMessage;
import ru.ifmo.diploma.synchronizer.protocol.handshake.Credentials;
import ru.ifmo.diploma.synchronizer.protocol.handshake.HandshakeMessage;
import ru.ifmo.diploma.synchronizer.protocol.handshake.RoutingTable;
import ru.ifmo.diploma.synchronizer.protocol.handshake.YesNoPackage;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;


/**
 * Created by ksenia on 21.05.2017.
 */
public class Discovery {
    private static final Logger LOG = LogManager.getLogger(Discovery.class);

    private int localPort;
    private String localAddr;
    private Map<String, CurrentConnections> connections = new ConcurrentHashMap<>();    //@TODO add all connections
    private Set<String> currentHostAddresses;
    private byte[] localMagicPackage = {5, 4, 3, 2};
    private BlockingQueue<AbstractMessage> tasks;

    //@TODO карта маршрутизации хранится на диске
    private Map<String, Credentials> authorizationTable = new HashMap<>();

    public Discovery(int localPort) {
        this.localPort = localPort;
    }

    public Discovery(int localPort, Map<String, Credentials> authorizationTable) {
        this.localPort = localPort;
        this.authorizationTable = authorizationTable;
        tasks = new LinkedBlockingQueue<>();
    }

    public int getLocalPort() {
        return localPort;
    }

    public String getLocalAddr() {
        return localAddr;
    }

    public Map<String, CurrentConnections> getConnections() {
        return connections;
    }

    public BlockingQueue<AbstractMessage> getTasks() {
        return tasks;
    }

    public void startDiscovery() {
        localAddr = "127.0.0.1:" + localPort;
        /*try {
            localAddr = InetAddress.getLocalHost().getHostAddress() + ":" + port;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }*/

        currentHostAddresses = currentHostAddresses();

        readRoutesAndConnect(authorizationTable);

        new AcceptThread().start(); //стартуем поток для входящих соединений
    }

    private void readRoutesAndConnect(Map<String, Credentials> authTable) {
        LOG.debug(localPort + " start routing");
        for (Map.Entry<String, Credentials> entry : authTable.entrySet()) {
            String addr = entry.getKey();

            if (checkIsCurrentHost(addr)) {
                continue;
            }

            Socket socket;
            if (connections.containsKey(addr)) {
                continue;
            }
            socket = new Socket(); //если такого подключения нет, то подключаемся

            try {
                socket.connect(parseAddress(addr));
                (new OutputConnectionThread(addr, socket)).start();

            } catch (IOException e) {
                Utils.closeSocket(socket);
//                System.err.println(localPort + ": Error connecting to " + addr);
                LOG.warn(localPort + ": Error connecting to " + addr);
            }
        }
    }

    private Set<String> currentHostAddresses() {
        currentHostAddresses = new HashSet<>();
        //получаем список доступных адресов на текущем хосте
        Enumeration<NetworkInterface> n;
        try {
            n = NetworkInterface.getNetworkInterfaces();

            while (n.hasMoreElements()) {
                NetworkInterface e = n.nextElement();
                Enumeration<InetAddress> a = e.getInetAddresses();
                while (a.hasMoreElements()) {
                    InetAddress inetAddress = a.nextElement();
                    currentHostAddresses.add(inetAddress.getHostAddress() + ":" + localPort);
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

    private class AcceptThread extends Thread {
        @Override
        public void run() {

            try (ServerSocket ssocket = new ServerSocket(localPort)) {   //стартуем на заданном порту
//                System.out.println(localPort + ": Server started on " + ssocket);
                LOG.debug(localPort + ": Server started on " + ssocket);

                while (!isInterrupted()) {
                    Socket socket = ssocket.accept();
                    new InputConnectionThread(socket).start();
                }
            } catch (IOException e) {
//                System.err.println(localPort + ": Server error");
                LOG.error(localPort + ": Server error");
                e.printStackTrace();
            }
        }
    }

    private SocketAddress parseAddress(String addr) {
        String[] split = addr.split(":");
        return new InetSocketAddress(split[0], Integer.parseInt(split[1]));
    }


    private class InputConnectionThread extends Thread {
        private String addr;
        private Socket socket;
        private boolean startReader = false;

        private InputConnectionThread(Socket socket) {
            super();
            this.socket = socket;
        }

        @Override
        public void run() {

            try {
                InputStream in = socket.getInputStream();
                OutputStream out = socket.getOutputStream();
//                read and check magic package
                byte[] buf = new byte[10];
                int len = in.read(buf);
                byte[] magicPackage = new byte[len];
                System.arraycopy(buf, 0, magicPackage, 0, len);

                if (!Arrays.equals(localMagicPackage, magicPackage)) {
                    return;
                }

                try {
                    ObjectInputStream objIn = new ObjectInputStream(in);
                    ObjectOutputStream objOut = new ObjectOutputStream(out);
                    while (!isInterrupted()) {

                        Object obj = objIn.readObject();

                        if (!(obj instanceof HandshakeMessage)) {
                            return;
                        }

                        if (obj instanceof Credentials) {

//                            System.out.println(obj + " from " + ((Credentials) obj).getFromAddr() + " to " + localPort);
                            LOG.debug(obj + " from " + ((Credentials) obj).getFromAddr() + " to " + localPort);

                            //credentials checking
                            addr = ((Credentials) obj).getFromAddr();
                            Credentials credFromTable = authorizationTable.get(addr);

                            if (credFromTable == null || obj.equals(credFromTable)) {
                                if ((connections.putIfAbsent(addr, new CurrentConnections(in, out, objIn, objOut))) != null) {
                                    LOG.debug(connections);
                                    objOut.writeObject(new YesNoPackage(localAddr, false, "Repeat connection"));
                                    objOut.flush();
                                    return;
                                }

                                objOut.writeObject(new YesNoPackage(localAddr, true, ""));
                                objOut.flush();
                                objOut.writeObject(new RoutingTable(localAddr, authorizationTable));
                                objOut.flush();
                            } else {
                                objOut.writeObject(new YesNoPackage(localAddr, false, "Invalid credentials "));
                                return;
                            }
                        } else if (obj instanceof RoutingTable) {
//                            System.out.println("Routing table from " + ((RoutingTable) obj).getFromAddr() + " to " + localPort);
                            LOG.debug("Routing table from " + ((RoutingTable) obj).getFromAddr() + " to " + localPort);
                            addHostsToMapAndConnect(((RoutingTable) obj).getAuthorizationTable());
                            break;
                        } else {
                            return;
                        }
                    }

                    //checking directory
                    startReader = true;
                    new TWriter(Discovery.this, socket, addr).start();
                    new TReader(Discovery.this, socket, addr).start();

                } catch (ClassNotFoundException e) {
//                    System.err.println(localPort + ": Data transferring error");
                    LOG.error(localPort + ": Data transferring error");
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (!startReader) {
//                    System.err.println(localPort + ": " + addr + " stopped");
                    LOG.error(localPort + ": " + addr + " stopped");
                    if (addr != null) {
                        connections.remove(addr);
                    }
                    Utils.closeSocket(socket);
                }
            }
        }
    }

    private class OutputConnectionThread extends Thread {
        private String addr;
        private Socket socket;
        private boolean startReader = false;

        private OutputConnectionThread(String addr, Socket socket) {
            super();
            this.addr = addr;
            this.socket = socket;
        }

        @Override
        public void run() {

            try {
                OutputStream out = socket.getOutputStream();
                InputStream in = socket.getInputStream();

//                System.out.println(localPort + ": magic to " + addr);
                LOG.debug(localPort + ": magic to " + addr);

                out.write(localMagicPackage);
                out.flush();

                try {
                    ObjectOutputStream objOut = new ObjectOutputStream(out);
                    ObjectInputStream objIn = new ObjectInputStream(in);

                    objOut.writeObject(new Credentials(localAddr, "login" + localPort, "password" + localPort));
                    objOut.flush();

                    //handshake
                    while (!isInterrupted()) {
                        Object obj = objIn.readObject();

                        if (!(obj instanceof HandshakeMessage)) {
                            return;
                        }

                        if (obj instanceof YesNoPackage) {
//                            System.out.println(obj + " " + ((YesNoPackage) obj).getStatus() + " from " + ((YesNoPackage) obj).getFromAddr() + " to " + localPort);
                            if (!((YesNoPackage) obj).getStatus()) {
                                return;
                            }
                            connections.put(addr, new CurrentConnections(in, out, objIn, objOut));
                            LOG.debug(connections);
//                            System.out.println(localPort + ": connected to " + addr);
                            LOG.info(localPort + ": connected to " + addr);

                            objOut.writeObject(new RoutingTable(localAddr, authorizationTable));
                            objOut.flush();
                        } else if (obj instanceof RoutingTable) {
//                            System.out.println("Routing table from " + ((RoutingTable) obj).getFromAddr() + " to " + localPort);
                            LOG.debug("Routing table from " + ((RoutingTable) obj).getFromAddr() + " to " + localPort);

                            addHostsToMapAndConnect(((RoutingTable) obj).getAuthorizationTable());
                            break;
                        }
                    }

                    //checking directory
                    startReader = true;
                    new TReader(Discovery.this, socket, addr).start();
                    new TWriter(Discovery.this, socket, addr).start();

                } catch (ClassNotFoundException e) {
//                    System.err.println(localPort + ": Data transferring error");
                    LOG.error(localPort + ": Data transferring error");
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (!startReader) {
//                    System.err.println(localPort + ": " + addr + " stopped");
                    LOG.error(localPort + ": " + addr + " stopped");
                    connections.remove(addr);
                    Utils.closeSocket(socket);
                }
            }
        }
    }

    private void addHostsToMapAndConnect(Map<String, Credentials> authTable) {
        Map<String, Credentials> diffAuthTable = new HashMap<>();
        diffAuthTable.putAll(authTable);
        diffAuthTable.entrySet().removeAll(authorizationTable.entrySet());
//        System.out.println(localPort + ": " + diffAuthTable);
        LOG.debug(localPort + ": " + diffAuthTable);

        if (diffAuthTable.isEmpty()) {
            return;
        }

        authorizationTable.putAll(diffAuthTable);
        readRoutesAndConnect(diffAuthTable);
    }

}
