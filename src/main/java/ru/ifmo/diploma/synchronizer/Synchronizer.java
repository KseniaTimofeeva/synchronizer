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

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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


    public Synchronizer(String localIP, int localPort, Map<String, Credentials> authorizationTable, String startPath) {
        localAddr = localIP + ":" + localPort;
        this.localPort = localPort;
        this.authorizationTable = authorizationTable;
        this.startPath = startPath;
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

    @Override
    public void run() {
        threadList.add(this);

        dc = new DirectoriesComparison(startPath, localAddr, writerTasks);

//        new Exit(this).start();
        Runtime.getRuntime().addShutdownHook(new Exit(this));

        //поток для отслеживания изменения директории в режиме реального времени
        Thread viewer = null;
        try {
            viewer = new Thread(new DirectoryChangesViewer(Paths.get(startPath), localAddr, fileOperations, writerTasks));
            viewer.start();
            threadList.add(viewer);
        } catch (IOException e) {
            //@TODO
        }

        addListeners();

        Thread worker = new Worker(this, readerTasks, listeners);
        worker.start(); //поток-обработчик входящих сообщений
        threadList.add(worker);

        Thread tWriter = new TWriter(this, writerTasks);
        tWriter.start(); //поток для отправки сообщений (в т.ч. broadcast)
        threadList.add(tWriter);

        discovery = new Discovery(this);
        discovery.startDiscovery();
    }

    public static void main(String[] args) {
        String localIP = null;
        try {
            localIP = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            LOG.error("Synchronizer can't identify local IP");
        }

        LOG.debug("Local IP: " + localIP);

        Map<String, Credentials> authorizationTable1 = new HashMap<>();
        authorizationTable1.put(localIP + ":60601", new Credentials("", "login60601", "password60601"));
        authorizationTable1.put(localIP + ":60602", new Credentials("", "login60602", "password60602"));
        authorizationTable1.put(localIP + ":60603", new Credentials("", "login60603", "password60603"));

        Map<String, Credentials> authorizationTable2 = new HashMap<>();
        authorizationTable2.put(localIP + ":60601", new Credentials("", "login60601", "password60601"));
        authorizationTable2.put(localIP + ":60603", new Credentials("", "login60603", "password60603"));

        Map<String, Credentials> authorizationTable3 = new HashMap<>();
        authorizationTable3.put(localIP + ":60602", new Credentials("", "login60602", "password60602"));
        authorizationTable3.put(localIP + ":60604", new Credentials("", "login60604", "password60604"));

        Map<String, Credentials> authorizationTable4 = new HashMap<>();
        authorizationTable4.put(localIP + ":60603", new Credentials("", "login60603", "password60603"));


        new Synchronizer(localIP, 60601, authorizationTable1, "C:\\synchronizer_work\\synchronized_1").start();
        threadSleep(3000);
        new Synchronizer(localIP, 60602, authorizationTable2, "C:\\synchronizer_work\\synchronized_2").start();
        threadSleep(3000);
        new Synchronizer(localIP, 60603, authorizationTable3, "C:\\synchronizer_work\\synchronized_3").start();
        threadSleep(3000);
        new Synchronizer(localIP, 60604, authorizationTable4, "C:\\synchronizer_work\\synchronized_4").start();

    }

    private static void threadSleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
