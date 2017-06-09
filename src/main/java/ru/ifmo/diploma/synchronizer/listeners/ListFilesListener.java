package ru.ifmo.diploma.synchronizer.listeners;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.ifmo.diploma.synchronizer.DirectoriesComparison;
import ru.ifmo.diploma.synchronizer.FileInfo;
import ru.ifmo.diploma.synchronizer.messages.*;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.BlockingQueue;

/*
 * Created by Юлия on 04.06.2017.
 */
public class ListFilesListener extends AbstractListener {
    private static final Logger LOG = LogManager.getLogger(ListFilesListener.class);

    private List<FileInfo> sentListFiles;

    public ListFilesListener(String localAddr, BlockingQueue<AbstractMsg> tasks, DirectoriesComparison dc) {
        super(localAddr, tasks, dc);
    }

    @Override
    public void handle(AbstractMsg msg) throws IOException, NoSuchAlgorithmException {

        if (msg.getType() == MessageType.LIST_FILES) {

            LOG.debug("{}: Listener: LIST_FILES from {}", localAddr, msg.getSender());

            if (((ListFilesMsg) msg).getListFiles() != null) {
                sentListFiles = ((ListFilesMsg) msg).getListFiles();
                tasks.offer(new ResultMsg(localAddr, msg.getSender(), MessageState.SUCCESS, msg));
                dc.compareDirectories(msg.getSender(), dc.getListFiles(),sentListFiles);
            } else {
                tasks.offer(new ResultMsg(localAddr, msg.getSender(), MessageState.FAILED, msg));
            }

        }
    }
}
