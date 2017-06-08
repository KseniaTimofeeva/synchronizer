package ru.ifmo.diploma.synchronizer.listeners;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.ifmo.diploma.synchronizer.DirectoriesComparison;
import ru.ifmo.diploma.synchronizer.messages.AbstractMsg;
import ru.ifmo.diploma.synchronizer.messages.ListFilesMsg;
import ru.ifmo.diploma.synchronizer.messages.MessageType;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.BlockingQueue;

/**
 * Created by ksenia on 08.06.2017.
 */
public class SendListFilesCommandListener extends AbstractListener {
    private static final Logger LOG = LogManager.getLogger(SendListFilesCommandListener.class);

    public SendListFilesCommandListener(String localAddr, BlockingQueue<AbstractMsg> tasks, DirectoriesComparison dc) {
        super(localAddr, tasks, dc);
    }

    @Override
    public void handle(AbstractMsg msg) throws IOException, NoSuchAlgorithmException {
        if (msg.getType() == MessageType.SEND_LIST_FILES_COMMAND) {

            LOG.debug("{}: Listener: send LIST_FILES to {}", localAddr, msg.getRecipient());

            tasks.offer(new ListFilesMsg(localAddr, msg.getRecipient(), dc.getListFiles()));
        }
    }
}
