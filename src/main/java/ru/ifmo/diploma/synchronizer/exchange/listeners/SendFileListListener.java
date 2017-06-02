package ru.ifmo.diploma.synchronizer.exchange.listeners;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.ifmo.diploma.synchronizer.discovery.CurrentConnections;
import ru.ifmo.diploma.synchronizer.protocol.exchange.AbstractMessage;
import ru.ifmo.diploma.synchronizer.protocol.exchange.ResponseMsg;
import ru.ifmo.diploma.synchronizer.protocol.exchange.SendFileListMessage;
import ru.ifmo.diploma.synchronizer.protocol.exchange.SendFileListResponse;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;

/**
 * Created by ksenia on 29.05.2017.
 */
public class SendFileListListener implements Listener<AbstractMessage> {
    private static final Logger LOG = LogManager.getLogger(SendFileListListener.class);

    private String localAddr;
    private BlockingQueue<AbstractMessage> tasks;

    public SendFileListListener(String localAddr, BlockingQueue<AbstractMessage> tasks) {
        this.localAddr = localAddr;
        this.tasks = tasks;
    }

    @Override
    public void handle(AbstractMessage msg, CurrentConnections currentConnections) {
        if (msg instanceof SendFileListMessage) {
            LOG.debug(localAddr + ": SendFileListMsg from " + msg.getFrom());

            tasks.offer(new ResponseMsg(localAddr));

        }
    }
}
