package ru.ifmo.diploma.synchronizer.exchange;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.ifmo.diploma.synchronizer.Synchronizer;
import ru.ifmo.diploma.synchronizer.discovery.CurrentConnections;
import ru.ifmo.diploma.synchronizer.Utils;
import ru.ifmo.diploma.synchronizer.messages.AbstractMsg;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

/**
 * Created by ksenia on 29.05.2017.
 */
public class TWriter extends Thread {
    private static final Logger LOG = LogManager.getLogger(TWriter.class);

    private Synchronizer synchronizer;
    private BlockingQueue<AbstractMsg> writerTasks;

    public TWriter(Synchronizer synchronizer, BlockingQueue<AbstractMsg> writerTasks) {
        this.synchronizer = synchronizer;
        this.writerTasks = writerTasks;
    }

    public void send(AbstractMsg msg) {
        writerTasks.offer(msg);
    }

    @Override
    public void run() {

        String localAddr = synchronizer.getLocalAddr();

        LOG.debug(localAddr + ": writer started");

        Map<String, CurrentConnections> connections = synchronizer.getConnections();


        try {
            LOG.trace(localAddr + ": writer is ready for writing");
            while (!isInterrupted()) {

                AbstractMsg msg = writerTasks.take();

                String addr = null;
                if (!msg.isBroadcast()) {

                    try {
                        LOG.debug(localAddr + ": writer: " + msg.getType() + " to " + msg.getRecipient());
                        addr = msg.getRecipient();
                        LOG.debug("{}: Writer: Connections {}", localAddr, connections);

                        if (connections.get(msg.getRecipient()) == null) {
                            LOG.debug("{}: Writer error. Connections error with {}", localAddr, msg.getRecipient());
                            continue;
                        }

                        ObjectOutputStream objOut = connections.get(msg.getRecipient()).getObjOut();
                        objOut.writeObject(msg);
                        objOut.flush();
                    } catch (IOException e) {
                        LOG.error(localAddr + ": Writer error. Write object error");
                        Utils.closeSocket(connections.remove(addr).getSocket());
                    }
                } else {
                    LOG.debug("{}: writer: broadcast {}", localAddr, msg.getType());
                    for (Map.Entry<String, CurrentConnections> entry : connections.entrySet()) {

                        try {
                            LOG.debug(localAddr + ": broadcast to " + entry.getKey());
                            addr = entry.getKey();
                            entry.getValue().getObjOut().writeObject(msg);
                            entry.getValue().getObjOut().flush();
                        } catch (IOException e) {
                            LOG.error(localAddr + ": Writer error. Write object error");
                            Utils.closeSocket(connections.remove(addr).getSocket());
                        }
                    }
                }
            }
        } catch (InterruptedException e) {
            LOG.trace(localAddr + ": Writer interrupted ");
            interrupt();
        } finally {
            if (!Utils.exit) {
                LOG.error(localAddr + ": Writer error");
            }
        }
    }
}
