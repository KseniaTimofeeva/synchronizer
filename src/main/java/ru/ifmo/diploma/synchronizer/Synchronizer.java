package ru.ifmo.diploma.synchronizer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.ifmo.diploma.synchronizer.discovery.CurrentConnections;
import ru.ifmo.diploma.synchronizer.discovery.Discovery;
import ru.ifmo.diploma.synchronizer.exchange.TWriter;
import ru.ifmo.diploma.synchronizer.exchange.Worker;
import ru.ifmo.diploma.synchronizer.listeners.CopyFileListener;
import ru.ifmo.diploma.synchronizer.listeners.DeleteFileListener;
import ru.ifmo.diploma.synchronizer.listeners.FileListener;
import ru.ifmo.diploma.synchronizer.listeners.Listener;
import ru.ifmo.diploma.synchronizer.listeners.RenameFileListener;
import ru.ifmo.diploma.synchronizer.listeners.ResultListener;
import ru.ifmo.diploma.synchronizer.listeners.SendFileRequestListener;
import ru.ifmo.diploma.synchronizer.listeners.ListFilesListener;
import ru.ifmo.diploma.synchronizer.listeners.SendListFilesCommandListener;
import ru.ifmo.diploma.synchronizer.listeners.TransferFileListener;
import ru.ifmo.diploma.synchronizer.messages.AbstractMsg;
import ru.ifmo.diploma.synchronizer.protocol.handshake.Credentials;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by ksenia on 20.05.2017.
 */
public class Synchronizer extends Thread {
    private static final Logger LOG = LogManager.getLogger(Synchronizer.class);

    private Map<String, Credentials> authorizationTable;
    private String startPath;
    private String localIP;
    private int localPort;
    private List<FileInfo> filesInfo = new ArrayList<>();
    private BlockingQueue<AbstractMsg> readerTasks = new LinkedBlockingQueue<>();
    private BlockingQueue<AbstractMsg> writerTasks = new LinkedBlockingQueue<>();
    private List<Listener<AbstractMsg>> listeners = new ArrayList<>();
    private Map<String, CurrentConnections> connections = new ConcurrentHashMap<>();
    private String localAddr;
    private DirectoriesComparison dc;
    private List<Thread> threadList = new ArrayList<>();
    private Discovery discovery;
    private BlockingQueue<FileOperation> fileOperations = new LinkedBlockingQueue<>();
    private Thread viewer;
    private String localLogin;
    private String localPassword;

    public Synchronizer(String localIP, int localPort, Map<String, Credentials> authorizationTable, String startPath,
                        String localLogin, String localPassword) {
        localAddr = localIP + ":" + localPort;
        this.localPort = localPort;
        this.authorizationTable = authorizationTable;
        this.startPath = startPath;
        this.localLogin = localLogin;
        this.localPassword = localPassword;
    }

    public String getLocalAddr() {
        return localAddr;
    }

    public Map<String, CurrentConnections> getConnections() {
        return connections;
    }

    public BlockingQueue<FileOperation> getFileOperations() {
        return fileOperations;
    }

    public int getLocalPort() {
        return localPort;
    }

    public Map<String, Credentials> getAuthorizationTable() {
        return authorizationTable;
    }

    public String getStartPath() {
        return startPath;
    }

    public List<FileInfo> getFilesInfo() {
        return filesInfo;
    }

    public BlockingQueue<AbstractMsg> getReaderTasks() {
        return readerTasks;
    }

    public BlockingQueue<AbstractMsg> getWriterTasks() {
        return writerTasks;
    }

    public DirectoriesComparison getDc() {
        return dc;
    }

    public List<Thread> getThreadList() {
        return threadList;
    }

    public ServerSocket getServerSocket() {
        return discovery.getServerSocket();
    }

    public Thread getViewer() {
        return viewer;
    }

    private void addListeners() {
        listeners.add(new CopyFileListener(localAddr, writerTasks, dc, fileOperations));
        listeners.add(new DeleteFileListener(localAddr, writerTasks, dc, fileOperations));
        listeners.add(new FileListener(localAddr, writerTasks, dc, fileOperations));
        listeners.add(new ListFilesListener(localAddr, writerTasks, dc));
        listeners.add(new RenameFileListener(localAddr, writerTasks, dc, fileOperations));
        listeners.add(new ResultListener(localAddr, writerTasks, dc));
        listeners.add(new SendFileRequestListener(localAddr, writerTasks, dc));
        listeners.add(new SendListFilesCommandListener(localAddr, writerTasks, dc));
        listeners.add(new TransferFileListener(localAddr, writerTasks, dc, fileOperations));
    }

    public void startSynchronizer() {

        threadList.add(this);

        dc = new DirectoriesComparison(startPath, localAddr, writerTasks);

        new Exit(this).start();

        //поток для отслеживания изменения директории в режиме реального времени
        try {
            viewer = new Thread(new DirectoryChangesViewer(Paths.get(startPath), localAddr, fileOperations, writerTasks, threadList));
            viewer.start();
        } catch (IOException e) {
//
        }

        addListeners();

        Thread worker = new Worker(this, readerTasks, listeners);
        worker.start(); //поток-обработчик входящих сообщений
        threadList.add(worker);

        Thread tWriter = new TWriter(this, writerTasks);
        tWriter.start(); //поток для отправки сообщений (в т.ч. broadcast)
        threadList.add(tWriter);

        discovery = new Discovery(this, localLogin, localPassword);
        discovery.startDiscovery();
    }

    public static void main(String[] args) {

        String startPath = null;

        Properties properties = new Properties();
        Map<String, Credentials> authorizationTable = new HashMap<>();

        int localPort;
        String localLogin;
        String localPassword;
        String localIP = null;

        try {
            FileInputStream fileIn = new FileInputStream("routes.properties");
            properties.load(fileIn);

            if (args == null || args.length == 0) {
                startPath = properties.getProperty("pc_0.startPath");
            } else {
                startPath = args[0];
            }
            if (startPath == null) {
                System.out.println("Please, specify the path of directory need to be synchronized");
                System.out.println("Use 'java -jar synchronizer.jar startpath' or specify it in file 'routes.properties'");
                return;
            }

//            startPath = startPath.replaceAll("\\\\", "/").trim();
            String lastChar = startPath.substring(startPath.length() - 1, startPath.length());
            if (lastChar.equals("\\") || lastChar.equals("/")) {
                startPath = startPath.substring(0, startPath.length() - 1); //удаляем последний сепаратор, если он есть
            }
            String[] addresses = properties.getProperty("address").split(";");
            String[] logins = properties.getProperty("login").split(";");
            String[] passwords = properties.getProperty("password").split(";");
            localPort = Integer.parseInt(properties.getProperty("pc_0.localPort"));
            localLogin = properties.getProperty("pc_0.login");
            localPassword = properties.getProperty("pc_0.password");
            localIP = properties.getProperty("pc_0.localIP");

            for (int i = 0; i < addresses.length; i++) {
                authorizationTable.put(addresses[i], new Credentials("", logins[i], passwords[i]));
            }

        } catch (FileNotFoundException e) {
            System.out.println("File 'routes.properties' not found in current directory");
            return;
        } catch (IOException e) {
            System.out.println("Can't load properties from file 'routes.properties'");
            return;
        }

        if (localIP == null || localIP.isEmpty()) {
            try {
                localIP = InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException e) {
                LOG.error("Synchronizer can't identify local IP");
            }
        }

        LOG.debug("Local IP: " + localIP);

        new Synchronizer(localIP, localPort, authorizationTable, startPath, localLogin, localPassword).startSynchronizer();
    }
}
