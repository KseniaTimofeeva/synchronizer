package ru.ifmo.diploma.synchronizer.discovery;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.ifmo.diploma.synchronizer.FileInfo;
import ru.ifmo.diploma.synchronizer.Synchronizer;
import ru.ifmo.diploma.synchronizer.Utils;
import ru.ifmo.diploma.synchronizer.exchange.TReader;
import ru.ifmo.diploma.synchronizer.messages.AbstractMsg;
import ru.ifmo.diploma.synchronizer.protocol.handshake.Credentials;
import ru.ifmo.diploma.synchronizer.protocol.handshake.HandshakeMessage;
import ru.ifmo.diploma.synchronizer.protocol.handshake.RoutingTable;
import ru.ifmo.diploma.synchronizer.protocol.handshake.YesNoPackage;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;


/**
 * Created by ksenia on 21.05.2017.
 */
public class Discovery {
    private static final Logger LOG = LogManager.getLogger(Discovery.class);

    private Synchronizer synchronizer;
    private int localPort;
    private Map<String, Credentials> authorizationTable;
    private String startPath;
    private List<FileInfo> filesInfo;
    private BlockingQueue<AbstractMsg> readerTasks;
    private BlockingQueue<AbstractMsg> writerTasks;
    private String localAddr;
    private Map<String, CurrentConnections> connections;
    private Set<String> currentHostAddresses;
    private ServerSocket serverSocket;
    private byte[] localMagicPackage = {5, 4, 3, 2};
    private String localLogin;
    private String localPassword;


    public Discovery(Synchronizer synchronizer, String localLogin, String localPassword) {
        this.synchronizer = synchronizer;
        localAddr = synchronizer.getLocalAddr();
        localPort = synchronizer.getLocalPort();
        authorizationTable = synchronizer.getAuthorizationTable();
        startPath = synchronizer.getStartPath();
        filesInfo = synchronizer.getFilesInfo();
        readerTasks = synchronizer.getReaderTasks();
        writerTasks = synchronizer.getWriterTasks();
        connections = synchronizer.getConnections();
        this.localLogin = localLogin;
        this.localPassword = localPassword;
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    public void startDiscovery() {

        currentHostAddresses = currentHostAddresses();

        readRoutesAndConnect(authorizationTable);

        startAccept(); //оставляем поток принимать входящие соединения
    }

    private void readRoutesAndConnect(Map<String, Credentials> authTable) {
        LOG.trace(localAddr + " start routing");
        for (Map.Entry<String, Credentials> entry : authTable.entrySet()) {
            String addr = entry.getKey();

            if (checkIsCurrentHost(addr)) {
                continue;
            }

            if (connections.containsKey(addr)) {
                continue;
            }
            Socket socket;
            socket = new Socket(); //если такого подключения нет, то подключаемся

            try {
                socket.connect(parseAddress(addr), 5000);
                (new OutputConnectionThread(addr, socket)).start();

            } catch (IOException e) {
                Utils.closeSocket(socket);
                LOG.warn(localAddr + ": Error connecting to " + addr + ". Cannot connect to remote host");
            }
        }
    }

    private Set<String> currentHostAddresses() {
        //получаем список доступных адресов на текущем хосте
        currentHostAddresses = new HashSet<>();
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
            LOG.error(localAddr + ": Cannot get local host addresses");
        }
        currentHostAddresses.add("127.0.1.1:" + localPort);
        return currentHostAddresses;
    }

    private boolean checkIsCurrentHost(String addr) {
        return currentHostAddresses.contains(addr);
    }

    private void startAccept() {

        try (ServerSocket ssocket = new ServerSocket(localPort)) {   //стартуем на заданном порту
            LOG.debug(localAddr + ": Server started on " + ssocket);
            serverSocket = ssocket;

            while (!Thread.currentThread().isInterrupted()) {
                Socket socket = ssocket.accept();
                new InputConnectionThread(socket).start();
            }
        } catch (IOException e) {
            if (!Utils.exit) {
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

        private InputConnectionThread(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {

            boolean toDelete = false;
            boolean readerStopped = false;

            try {
                InputStream in = socket.getInputStream();
                OutputStream out = socket.getOutputStream();
//                read and check magic package
                byte[] buf = new byte[10];
                int len = in.read(buf);
                byte[] magicPackage = new byte[len];
                System.arraycopy(buf, 0, magicPackage, 0, len);

                LOG.trace(localAddr + ": Received magic package " + Arrays.toString(magicPackage));

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
                            addr = ((Credentials) obj).getFromAddr();

                            LOG.debug(obj + " from " + ((Credentials) obj).getFromAddr() + " to " + localAddr);

                            if ((connections.putIfAbsent(addr, new CurrentConnections(this, socket, objIn, objOut))) != null) {
                                objOut.writeObject(new YesNoPackage(localAddr, false, "Repeat connection"));
                                objOut.flush();
                                LOG.warn(localAddr + ": Host " + addr + " is already connected");
                                return;
                            }
                            LOG.trace(localAddr + ": List of connections " + connections);

                            //credentials checking
                            Credentials credFromTable = authorizationTable.get(addr);

                            if (credFromTable == null || obj.equals(credFromTable)) {
                                objOut.writeObject(new YesNoPackage(localAddr, true, ""));
                                objOut.flush();
                                objOut.writeObject(new RoutingTable(localAddr, authorizationTable));
                                objOut.flush();
                            } else {
                                objOut.writeObject(new YesNoPackage(localAddr, false, "Invalid credentials "));
                                LOG.error(localAddr + ": Invalid credentials from " + addr);
                                toDelete = true;
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

                    //current thread starts reading msgs
                    new TReader(this, synchronizer, readerTasks, socket, addr).startReader();
                    readerStopped = true;
                } catch (ClassNotFoundException e) {
                    toDelete = true;
                    LOG.error(localAddr + ": Data transferring error");
                }
            } catch (IOException e) {
                if (addr != null) {
                    toDelete = true;
                    LOG.error(localAddr + ": Connection error with host " + addr);
                } else {
                    LOG.error(localAddr + ": Socket error " + socket);
                }
            } finally {
                if (toDelete) {
                    connections.remove(addr);
                }
                if (!Utils.exit) {
                    if (!readerStopped) {
                        LOG.error(localAddr + ": Remote host is not authorized. Socket closed");
                    }
                    LOG.error(localAddr + ": " + addr + " stopped");
                }
                Utils.closeSocket(socket);
            }
        }
    }

    private class OutputConnectionThread extends Thread {
        private String addr;
        private Socket socket;

        private OutputConnectionThread(String addr, Socket socket) {
            this.addr = addr;
            this.socket = socket;
        }

        @Override
        public void run() {
            if (connections.putIfAbsent(addr, new CurrentConnections(this, socket)) != null) {
                return;
            }

            try {
                OutputStream out = socket.getOutputStream();
                InputStream in = socket.getInputStream();


                LOG.debug(localAddr + ": magic to " + addr);

                out.write(localMagicPackage);
                out.flush();
                try {
                    sleep(300);
                    ObjectOutputStream objOut = new ObjectOutputStream(out);
                    ObjectInputStream objIn = new ObjectInputStream(in);

                    objOut.writeObject(new Credentials(localAddr, localLogin, localPassword));
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
                            connections.get(addr).setObjInObjOut(objIn, objOut);
                            LOG.trace(localAddr + ": List of connections " + connections);
                            LOG.info(localAddr + ": connected to " + addr);

                            objOut.writeObject(new RoutingTable(localAddr, authorizationTable));
                            objOut.flush();
                        } else if (obj instanceof RoutingTable) {
                            LOG.debug("Routing table from " + ((RoutingTable) obj).getFromAddr() + " to " + localAddr);

                            addHostsToMapAndConnect(((RoutingTable) obj).getAuthorizationTable());
                            break;
                        }
                    }

                    //current thread starts reading msgs
                    new TReader(this, synchronizer, readerTasks, socket, addr).startReader();

                } catch (ClassNotFoundException e) {
                    LOG.error(localAddr + ": Data transferring error from " + addr);
                } catch (InterruptedException e) {
                    LOG.error(localAddr + ": interrupted ");
                    interrupt();
                }
            } catch (IOException e) {
                LOG.error(localAddr + ": Connection error with host " + addr);
            } finally {
                if (!Utils.exit) {
                    LOG.error(localAddr + ": " + addr + " stopped");
                }
                connections.remove(addr);
                Utils.closeSocket(socket);
            }
        }
    }

    private void addHostsToMapAndConnect(Map<String, Credentials> authTable) {
        Map<String, Credentials> diffAuthTable = new HashMap<>();
        diffAuthTable.putAll(authTable);
        diffAuthTable.entrySet().removeAll(authorizationTable.entrySet());
        LOG.trace(localAddr + ": " + diffAuthTable);

        if (diffAuthTable.isEmpty()) {
            return;
        }

        authorizationTable.putAll(diffAuthTable);
        readRoutesAndConnect(diffAuthTable);
    }

}
