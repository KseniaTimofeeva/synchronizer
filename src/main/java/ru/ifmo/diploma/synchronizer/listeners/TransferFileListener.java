package ru.ifmo.diploma.synchronizer.listeners;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.ifmo.diploma.synchronizer.DirectoriesComparison;
import ru.ifmo.diploma.synchronizer.FileOperation;
import ru.ifmo.diploma.synchronizer.OperationType;
import ru.ifmo.diploma.synchronizer.messages.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

/*
 * Created by Юлия on 04.06.2017.
 */
public class TransferFileListener extends AbstractListener {
    private static final Logger LOG = LogManager.getLogger(TransferFileListener.class);
    BlockingQueue<FileOperation> fileOperations;

    public TransferFileListener(String localAddr, BlockingQueue<AbstractMsg> tasks, DirectoriesComparison dc, BlockingQueue<FileOperation> fileOperations) {
        super(localAddr, tasks, dc);
        this.fileOperations = fileOperations;
    }

    @Override
    public void handle(AbstractMsg msg) {
        if (msg.getType() == MessageType.TRANSFER_FILE) {

            TransferFileMsg transMsg = (TransferFileMsg) msg;
            LOG.debug("{}: Listener: send TRANSFER_FILE {} to {}", localAddr, transMsg.getOldRelativePath(), msg.getRecipient());
            fileOperations.add(new FileOperation(OperationType.ENTRY_MOVE, transMsg.getOldRelativePath()));

            Path oldPath = Paths.get(dc.getAbsolutePath(transMsg.getOldRelativePath()));
            String p = dc.getAbsolutePath(transMsg.getNewRelativePath());
            Path newPath = Paths.get(p);
            Path newDirPath = Paths.get(p.substring(0, p.lastIndexOf("\\")));

            try {
                if (!Files.exists(newDirPath)) {
                    Files.createDirectories(newDirPath);
                }

                /*if (!fileOperations.containsKey(msg.getRecipient()))
                    fileOperations.put(msg.getRecipient(), new ArrayList<>());
                fileOperations.get(fileOperations).add(new FileOperation(OperationType.ENTRY_MOVE, transMsg.getNewRelativePath()));
*/


                Files.move(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                tasks.offer(new ResultMsg(msg.getSender(), msg.getSender(), MessageState.FAILED, msg));

                e.printStackTrace();
            }
        }
    }
}
