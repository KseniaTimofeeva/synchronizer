package ru.ifmo.diploma.synchronizer.exchange.listeners;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.ifmo.diploma.synchronizer.discovery.CurrentConnections;
import ru.ifmo.diploma.synchronizer.protocol.exchange.AbstractMessage;
import ru.ifmo.diploma.synchronizer.protocol.exchange.SendFileListMessage;
import ru.ifmo.diploma.synchronizer.protocol.exchange.SendFileListResponse;

import java.io.IOException;

/**
 * Created by ksenia on 29.05.2017.
 */
public class SendFileListListener implements Listener<AbstractMessage> {
    private static final Logger LOG = LogManager.getLogger(SendFileListListener.class);

    private String localAddr;

    @Override
    public void handle(AbstractMessage msg, CurrentConnections currentConnections, String localAddr) throws IOException {
        if (msg instanceof SendFileListMessage) {
            LOG.debug(localAddr + ": SendFileListMsg from " + msg.getFrom());
            currentConnections.getObjOut().writeObject(new SendFileListResponse(localAddr));

        }
    }
}
