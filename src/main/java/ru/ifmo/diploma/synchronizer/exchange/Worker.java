package ru.ifmo.diploma.synchronizer.exchange;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.ifmo.diploma.synchronizer.Synchronizer;
import ru.ifmo.diploma.synchronizer.Utils;
import ru.ifmo.diploma.synchronizer.discovery.CurrentConnections;
import ru.ifmo.diploma.synchronizer.listeners.Listener;
import ru.ifmo.diploma.synchronizer.messages.AbstractMsg;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

/**
 * Created by ksenia on 08.06.2017.
 */
public class Worker extends Thread {
    private static final Logger LOG = LogManager.getLogger(Worker.class);

    private Synchronizer synchronizer;
    private BlockingQueue<AbstractMsg> readerTasks;
    private List<Listener<AbstractMsg>> listeners;


    public Worker(Synchronizer synchronizer, BlockingQueue<AbstractMsg> readerTasks, List<Listener<AbstractMsg>> listeners) {
        this.synchronizer = synchronizer;
        this.readerTasks = readerTasks;
        this.listeners = listeners;
    }

    private void notifyListeners(AbstractMsg msg) throws IOException, NoSuchAlgorithmException {
        for (Listener<AbstractMsg> listener : listeners) {
            listener.handle(msg);
        }
    }

    @Override
    public void run() {

        String localAddr = synchronizer.getLocalAddr();

        LOG.debug(localAddr + ": worker started");

        Map<String, CurrentConnections> connections = synchronizer.getConnections();

        try {
            while (!isInterrupted()) {

                AbstractMsg msg = readerTasks.take();

                LOG.trace(localAddr + ": worker: " + msg.getType() + " from " + msg.getSender());
                notifyListeners(msg);
            }
        } catch (InterruptedException e) {
            LOG.trace(localAddr + ": Worker interrupted ");
            interrupt();
        } catch (NoSuchAlgorithmException | IOException e) {
            LOG.debug(localAddr + ": Listeners error. Can't get file list");
        } finally {
            if (!Utils.exit) {
                LOG.error(localAddr + ": Worker error");
            }
            for (CurrentConnections con : connections.values()) {
                Utils.closeSocket(con.getSocket());
            }
        }
    }
}
