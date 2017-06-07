package ru.ifmo.diploma.synchronizer.exchange;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.ifmo.diploma.synchronizer.DirectoriesComparison;
import ru.ifmo.diploma.synchronizer.Utils;
import ru.ifmo.diploma.synchronizer.discovery.CurrentConnections;
import ru.ifmo.diploma.synchronizer.discovery.Discovery;
import ru.ifmo.diploma.synchronizer.listeners.CopyFileListener;
import ru.ifmo.diploma.synchronizer.listeners.DeleteFileListener;
import ru.ifmo.diploma.synchronizer.listeners.Listener;
import ru.ifmo.diploma.synchronizer.listeners.RenameFileListener;
import ru.ifmo.diploma.synchronizer.listeners.SendFileRequestListener;
import ru.ifmo.diploma.synchronizer.listeners.SendListFilesListener;
import ru.ifmo.diploma.synchronizer.listeners.TransferFileListener;
import ru.ifmo.diploma.synchronizer.messages.AbstractMsg;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

/**
 * Created by ksenia on 29.05.2017.
 */
public class TReader extends Thread {
    private static final Logger LOG = LogManager.getLogger(TReader.class);

    private Discovery discovery;
    private Socket socket;
    private String addr;
    private List<Listener<AbstractMsg>> listeners = new ArrayList<>();
    private String localAddr;
    private CurrentConnections currentConnections;
    private BlockingQueue<AbstractMsg> tasks;
    private DirectoriesComparison dc = new DirectoriesComparison();

    public TReader(Discovery discovery, Socket socket, String addr) {
        this.discovery = discovery;
        this.socket = socket;
        this.addr = addr;
    }

    private void addListeners() {
        listeners.add(new CopyFileListener(localAddr, tasks, dc));
        listeners.add(new DeleteFileListener(localAddr, tasks, dc));
        listeners.add(new RenameFileListener(localAddr, tasks, dc));
        listeners.add(new SendFileRequestListener(localAddr, tasks, dc, currentConnections.getObjOut()));
        listeners.add(new SendListFilesListener(localAddr, tasks, dc, currentConnections.getObjOut()));
        listeners.add(new TransferFileListener(localAddr, tasks, dc));
    }

    private void notifyListeners(AbstractMsg msg) {
        for (Listener<AbstractMsg> listener : listeners) {
            listener.handle(msg);
        }
    }

    @Override
    public void run() {

        int localPort = discovery.getLocalPort();
        localAddr = discovery.getLocalAddr();

        LOG.debug(localAddr + ": reader to " + addr + " started");

        tasks = discovery.getTasks();
        List<Socket> socketList = discovery.getSocketList();

        addListeners();

        Map<String, CurrentConnections> connections = discovery.getConnections();
        CurrentConnections currentConnections = connections.get(addr);

        LOG.debug(localAddr + ": reader: Current connections: " + currentConnections + " with host " + addr);

        ObjectInputStream objIn = currentConnections.getObjIn();
//        ObjectOutputStream objOut = currentConnections.getObjOut();

        try {
            LOG.debug(localAddr + ": ready for reading from " + addr);
            while (!isInterrupted()) {

                Object obj = objIn.readObject();

                if (!(obj instanceof AbstractMsg)) {
                    LOG.error(localAddr + ": Unexpected message");
                    return;
                }
                LOG.debug(localAddr + ": reader: AbstrMsg " + obj + " from " + ((AbstractMsg) obj).getSender());
                notifyListeners((AbstractMsg) obj);
            }
        } catch (IOException | ClassNotFoundException e) {
            LOG.error(localAddr + ": Reader error. Read object error");
            LOG.debug("reader: " + Arrays.toString(e.getStackTrace()));
        } finally {
            LOG.error(localAddr + ": Reader error. " + addr + " stopped");
            connections.remove(addr);
            socketList.remove(socket);
            Utils.closeSocket(socket);
        }


    }
}
