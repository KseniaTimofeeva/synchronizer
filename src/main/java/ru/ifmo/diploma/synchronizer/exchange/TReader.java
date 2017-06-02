package ru.ifmo.diploma.synchronizer.exchange;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.ifmo.diploma.synchronizer.Utils;
import ru.ifmo.diploma.synchronizer.discovery.CurrentConnections;
import ru.ifmo.diploma.synchronizer.discovery.Discovery;
import ru.ifmo.diploma.synchronizer.exchange.listeners.Listener;
import ru.ifmo.diploma.synchronizer.exchange.listeners.SendFileListListener;
import ru.ifmo.diploma.synchronizer.exchange.listeners.SendFileListener;
import ru.ifmo.diploma.synchronizer.protocol.exchange.AbstractMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

/**
 * Created by ksenia on 29.05.2017.
 */
public class TReader extends Thread {
    private static final Logger LOG = LogManager.getLogger(TReader.class);

    private CountDownLatch latch;
    private Discovery discovery;
    private Socket socket;
    private String addr;
    private List<Listener<AbstractMessage>> listeners = new ArrayList<>();
    private String localAddr;
    private CurrentConnections currentConnections;
    private BlockingQueue<AbstractMessage> tasks;

    public TReader(Discovery discovery, Socket socket, String addr) {
        this.discovery = discovery;
        this.socket = socket;
        this.addr = addr;
    }

    private void addListeners() {
        listeners.add(new SendFileListener(localAddr, tasks));
        listeners.add(new SendFileListListener(localAddr, tasks));
    }

    private void notifyListeners(AbstractMessage msg) {
        for (Listener<AbstractMessage> listener : listeners) {
            listener.handle(msg, currentConnections);   //@TODO add inputstream
        }
    }

    @Override
    public void run() {

        int localPort = discovery.getLocalPort();
        localAddr = discovery.getLocalAddr();

        LOG.debug(localAddr + ": reader to " + addr + " started");

        tasks = discovery.getTasks();

        addListeners();

        Map<String, CurrentConnections> connections = discovery.getConnections();
        CurrentConnections currentConnections = connections.get(addr);

        LOG.debug(localAddr + ": reader: Current connections: " + currentConnections + " with host " + addr);

        InputStream in = currentConnections.getIn();
//        OutputStream out = currentConnections.getOut();
        ObjectInputStream objIn = currentConnections.getObjIn();
//        ObjectOutputStream objOut = currentConnections.getObjOut();

        try {
            LOG.debug(localAddr + ": ready for reading from " + addr);
            while (!isInterrupted()) {

                Object obj = objIn.readObject();

                if (!(obj instanceof AbstractMessage)) {
                    LOG.error(localAddr + ": Unexpected mesage");
                    return;
                }
                LOG.debug(localAddr + ": reader: AbstrMsg " + obj + " from " + ((AbstractMessage) obj).getFrom());
                notifyListeners((AbstractMessage) obj);
            }
        } catch (IOException | ClassNotFoundException e) {
            LOG.error(localAddr + ": Reader error. Read object error");
            LOG.error(e.getStackTrace());
        } finally {
            LOG.error(localAddr + ": Reader error. " + addr + " stopped");
            connections.remove(addr);
            Utils.closeSocket(socket);
        }


    }
}
