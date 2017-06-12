package ru.ifmo.diploma.synchronizer.listeners;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.ifmo.diploma.synchronizer.DirectoriesComparison;
import ru.ifmo.diploma.synchronizer.messages.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.BlockingQueue;

/*
 * Created by Юлия on 04.06.2017.
 */
public class CopyFileListener extends AbstractListener {
    private static final Logger LOG = LogManager.getLogger(CopyFileListener.class);

    public CopyFileListener(String localAddr, BlockingQueue<AbstractMsg> tasks, DirectoriesComparison dc) {
        super(localAddr, tasks, dc);
    }

    @Override
    public void handle(AbstractMsg msg) {
        if (msg.getType() == MessageType.COPY_FILE) {

            CopyFileMsg copyMsg = (CopyFileMsg) msg;
            LOG.debug("{}: Listener: COPY_FILE {} from {}", localAddr, copyMsg.getRelativePath(), msg.getSender());

            Path oldPath = Paths.get(dc.getAbsolutePath(copyMsg.getRelativePath()));
            String p = dc.getAbsolutePath(copyMsg.getNewRelativePath());
            Path newPath = Paths.get(p);
            Path newDirPath = Paths.get(p.substring(0, p.lastIndexOf(File.separator)));

            try {
                if (!Files.exists(newDirPath)) {
                    Files.createDirectories(newDirPath);
                }

                Files.copy(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
            }
            catch (IOException e) {
                if (!msg.isBroadcast()) {
                    tasks.offer(new ResultMsg(localAddr, msg.getSender(), MessageState.FAILED, msg));
                }

                e.printStackTrace();
            }

            dc.setCreationTime(newPath.toString(), copyMsg.getCreationTime());
            if (!msg.isBroadcast()) {
                tasks.offer(new ResultMsg(localAddr, msg.getSender(), MessageState.SUCCESS, msg));
            }
        }
    }
}
