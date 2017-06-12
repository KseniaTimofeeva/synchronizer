package ru.ifmo.diploma.synchronizer.listeners;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.ifmo.diploma.synchronizer.DirectoriesComparison;
import ru.ifmo.diploma.synchronizer.messages.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.BlockingQueue;

/*
 * Created by Юлия on 04.06.2017.
 */
public class DeleteFileListener extends AbstractListener {
    private static final Logger LOG = LogManager.getLogger(DeleteFileListener.class);

    public DeleteFileListener(String localAddr, BlockingQueue<AbstractMsg> tasks, DirectoriesComparison dc) {
        super(localAddr, tasks, dc);
    }

    @Override
    public void handle(AbstractMsg msg) {
        if (msg.getType() == MessageType.DELETE_FILE) {

            DeleteFileMsg delMsg = (DeleteFileMsg) msg;
            LOG.debug("{}: Listener: DELETE_FILE {} from {}", localAddr, delMsg.getRelativePath(), msg.getSender());

            try {
                Files.deleteIfExists(Paths.get(dc.getAbsolutePath(delMsg.getRelativePath())));
                if (!msg.isBroadcast()) {
                    tasks.offer(new ResultMsg(localAddr, msg.getSender(), MessageState.SUCCESS, msg));
                }

            } catch (IOException e) {
                if (!msg.isBroadcast()) {
                    tasks.offer(new ResultMsg(localAddr, msg.getSender(), MessageState.FAILED, msg));
                }
                e.printStackTrace();
            }
        }

    }
}
