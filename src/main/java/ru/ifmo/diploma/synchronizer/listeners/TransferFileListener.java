package ru.ifmo.diploma.synchronizer.listeners;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.ifmo.diploma.synchronizer.DirectoriesComparison;
import ru.ifmo.diploma.synchronizer.messages.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.BlockingQueue;

/*
 * Created by Юлия on 04.06.2017.
 */
public class TransferFileListener extends AbstractListener {
    private static final Logger LOG = LogManager.getLogger(TransferFileListener.class);

    public TransferFileListener(String localAddr, BlockingQueue<AbstractMsg> tasks, DirectoriesComparison dc) {
        super(localAddr, tasks, dc);
    }

    @Override
    public void handle(AbstractMsg msg) {
        if (msg.getType() == MessageType.TRANSFER_FILE) {

            TransferFileMsg transMsg = (TransferFileMsg) msg;
            LOG.debug("{}: Listener: TRANSFER_FILE {} from {}", localAddr, transMsg.getOldRelativePath(), msg.getSender());

            Path oldPath = Paths.get(dc.getAbsolutePath(transMsg.getOldRelativePath()));
            String p = dc.getAbsolutePath(transMsg.getNewRelativePath());
            Path newPath = Paths.get(p);
            Path newDirPath = Paths.get(p.substring(0, p.lastIndexOf(File.separator)));

            try {
                if (!Files.exists(newDirPath)) {
                    Files.createDirectories(newDirPath);
                }

                Files.move(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                if (!msg.isBroadcast()) {
                    tasks.offer(new ResultMsg(msg.getSender(), msg.getSender(), MessageState.FAILED, msg));
                }

                e.printStackTrace();
            }
        }
    }
}
