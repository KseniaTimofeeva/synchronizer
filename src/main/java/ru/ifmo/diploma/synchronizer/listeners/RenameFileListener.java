package ru.ifmo.diploma.synchronizer.listeners;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.ifmo.diploma.synchronizer.DirectoriesComparison;
import ru.ifmo.diploma.synchronizer.FileOperation;
import ru.ifmo.diploma.synchronizer.OperationType;
import ru.ifmo.diploma.synchronizer.messages.*;

import java.io.File;
import java.util.concurrent.BlockingQueue;

/*
 * Created by Юлия on 04.06.2017.
 */
public class RenameFileListener extends AbstractListener {
    private static final Logger LOG = LogManager.getLogger(RenameFileListener.class);
    private BlockingQueue<FileOperation> fileOperations;

    public RenameFileListener(String localAddr, BlockingQueue<AbstractMsg> tasks, DirectoriesComparison dc, BlockingQueue<FileOperation> fileOperations) {
        super(localAddr, tasks, dc);
        this.fileOperations = fileOperations;
    }

    @Override
    public void handle(AbstractMsg msg) {
        if (msg.getType() == MessageType.RENAME_FILE) {

            RenameFileMsg renameMsg = (RenameFileMsg) msg;
            LOG.debug("{}: Listener: RENAME_FILE {} to {} from host {}", localAddr, renameMsg.getOldRelativePath(),
                    renameMsg.getNewRelativePath(), msg.getSender());


            fileOperations.add(new FileOperation(OperationType.ENTRY_RENAME, dc.getAbsolutePath(renameMsg.getOldRelativePath())));

            File oldFile = new File(dc.getAbsolutePath(renameMsg.getOldRelativePath()));
            File newFile = new File(dc.getAbsolutePath(renameMsg.getNewRelativePath()));
            if (!oldFile.renameTo(newFile)) {
                if (!msg.isBroadcast()) {
                    tasks.offer(new ResultMsg(localAddr, msg.getSender(), MessageState.FAILED, msg));
                }
            } else {
                if (!msg.isBroadcast()) {
                    tasks.offer(new ResultMsg(localAddr, msg.getSender(), MessageState.SUCCESS, msg));
                }
            }
        }
    }
}
