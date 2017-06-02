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
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;


/**
 * Created by ksenia on 21.05.2017.
 */
public class Discovery extends Thread{
    private static final Logger LOG = LogManager.getLogger(Discovery.class);

    private int localPort;
    private String localAddr;
    private Map<String, CurrentConnections> connections = new ConcurrentHashMap<>();    //@TODO add all connections
    private Set<String> currentHostAddresses;
    private byte[] localMagicPackage = {5, 4, 3, 2};
    private BlockingQueue<AbstractMessage> tasks;
    private List<Socket> socketList = new CopyOnWriteArrayList<>();

    private Map<String, Credentials> authorizationTable;

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

    public List<Socket> getSocketList() {
        return socketList;
    }

    @Override
    public void run() {

        try {
            localAddr = InetAddress.getLocalHost().getHostAddress() + ":" + localPort;
        } catch (UnknownHostException e) {
            LOG.debug("discovery: " + Arrays.toString(e.getStackTrace()));
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                for (Socket s : socketList) {
                    Utils.closeSocket(s);
                }
                System.out.println("End host " + localAddr);
            }
        });

        currentHostAddresses = currentHostAddresses();

        readRoutesAndConnect(authorizationTable);

        new AcceptThread().start(); //стартуем поток для входящих соединений
    }

    private void readRoutesAndConnect(Map<String, Credentials> authTable) {
        LOG.debug(localAddr + " start routing");
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
                LOG.warn(localAddr + ": Error connecting to " + addr + ". Cannot connect to remote host");
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
            LOG.debug(localAddr + ": Cannot get local host addresses " + Arrays.toString(e.getStackTrace()));
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
                LOG.debug(localAddr + ": Server started on " + ssocket);

                while (!isInterrupted()) {
                    Socket socket = ssocket.accept();
                    new InputConnectionThread(socket).start();
                }
            } catch (IOException e) {
                LOG.error(localAddr + ": Server error");
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

                LOG.debug(localAddr + ": Received magic package " + Arrays.toString(magicPackage));

                if (!Arrays.equals(localMagicPackage, magicPackage)) {
                    LOG.error(localAddr + ": Wrong magic package");
                    return;
                }

                try {
                    ObjectInputStream objIn = new ObjectInputStream(in);
                    ObjectOutputStream objOut = new ObjectOutputStream(out);
                    while (!isInterrupted()) {

                        Object obj = objIn.readObject();

                        if (!(obj instanceof HandshakeMessage)) {
                            LOG.error(localAddr + ": No handshake message. Handshake message is expected");
                            return;
                        }

                        if (obj instanceof Credentials) {

                            LOG.debug(obj + " from " + ((Credentials) obj).getFromAddr() + " to " + localAddr);

                            //credentials checking
                            addr = ((Credentials) obj).getFromAddr();
                            Credentials credFromTable = authorizationTable.get(addr);

                            if (credFromTable == null || obj.equals(credFromTable)) {
                                if ((connections.putIfAbsent(addr, new CurrentConnections(in, out, objIn, objOut))) != null) {
                                    objOut.writeObject(new YesNoPackage(localAddr, false, "Repeat connection"));
                                    objOut.flush();
                                    LOG.warn(localAddr + ": Host " + addr + " is already connected");
                                    return;
                                }
                                socketList.add(socket);
                                LOG.debug(localAddr + ": List of connections " + connections);
                                objOut.writeObject(new YesNoPackage(localAddr, true, ""));
                                objOut.flush();
                                objOut.writeObject(new RoutingTable(localAddr, authorizationTable));
                                objOut.flush();
                            } else {
                                objOut.writeObject(new YesNoPackage(localAddr, false, "Invalid credentials "));
                                LOG.error(localAddr + ": Invalid credentials from " + addr);
                                return;
                            }
                        } else if (obj instanceof RoutingTable) {
                            LOG.debug("Routing table from " + ((RoutingTable) obj).getFromAddr() + " to " + localAddr);
                            addHostsToMapAndConnect(((RoutingTable) obj).getAuthorizationTable());
                            break;
                        } else {
                            LOG.error(localAddr + ": Unexpected message from " + addr);
                            return;
                        }
                    }

                    //checking directory
                    startReader = true;
                    new TWriter(Discovery.this, socket, addr).start();
                    new TReader(Discovery.this, socket, addr).start();

                } catch (ClassNotFoundException e) {
                    LOG.error(localAddr + ": Data transferring error");
                }
            } catch (IOException e) {
                if (addr != null) {
                    LOG.error(localAddr + ": Connection error with host " + addr);
                } else {
                    LOG.error(localAddr + ": Socket error " + socket);
                }
            } finally {
                if (!startReader) {
                    LOG.error(localAddr + ": Remote host is not authorized. Socket closed");
                    if (addr != null) {
                        LOG.error(localAddr + ": " + addr + " stopped");
                        connections.remove(addr);
                        socketList.remove(socket);
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

                LOG.debug(localAddr + ": magic to " + addr);

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
                            LOG.warn(localAddr + ": No handshake message. Handshake message is expected");
                            return;
                        }

                        if (obj instanceof YesNoPackage) {
                            if (!((YesNoPackage) obj).getStatus()) {
                                LOG.warn(localAddr + ": Not authorized by " + ((YesNoPackage) obj).getFromAddr());
                                return;
                            }
                            connections.put(addr, new CurrentConnections(in, out, objIn, objOut));
                            socketList.add(socket);
                            LOG.debug(localAddr + ": List of connections " + connections);
                            LOG.info(localAddr + ": connected to " + addr);

                            objOut.writeObject(new RoutingTable(localAddr, authorizationTable));
                            objOut.flush();
                        } else if (obj instanceof RoutingTable) {
                            LOG.debug("Routing table from " + ((RoutingTable) obj).getFromAddr() + " to " + localAddr);

                            addHostsToMapAndConnect(((RoutingTable) obj).getAuthorizationTable());
                            break;
                        }
                    }

                    //checking directory
                    startReader = true;
                    new TReader(Discovery.this, socket, addr).start();
                    new TWriter(Discovery.this, socket, addr).start();

                } catch (ClassNotFoundException e) {
                    LOG.error(localAddr + ": Data transferring error from " + addr);
                }
            } catch (IOException e) {
                LOG.error(localAddr + ": Connection error with host " + addr);
            } finally {
                if (!startReader) {
                    LOG.error(localAddr + ": " + addr + " stopped");
                    connections.remove(addr);
                    socketList.remove(socket);
                    Utils.closeSocket(socket);
                }
            }
        }
    }

    private void addHostsToMapAndConnect(Map<String, Credentials> authTable) {
        Map<String, Credentials> diffAuthTable = new HashMap<>();
        diffAuthTable.putAll(authTable);
        diffAuthTable.entrySet().removeAll(authorizationTable.entrySet());
        LOG.debug(localAddr + ": " + diffAuthTable);

        if (diffAuthTable.isEmpty()) {
            return;
        }

        authorizationTable.putAll(diffAuthTable);
        readRoutesAndConnect(diffAuthTable);
    }

}
