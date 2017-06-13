package ru.ifmo.diploma.synchronizer.listeners;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.ifmo.diploma.synchronizer.DirectoriesComparison;
import ru.ifmo.diploma.synchronizer.FileOperation;
import ru.ifmo.diploma.synchronizer.OperationType;
import ru.ifmo.diploma.synchronizer.messages.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;

/*
 * Created by Юлия on 06.06.2017.
 */
public class FileListener extends AbstractListener{
    private static final Logger LOG = LogManager.getLogger(FileListener.class);
    private BlockingQueue<FileOperation> fileOperations;

    public FileListener(String localAddr, BlockingQueue<AbstractMsg> tasks, DirectoriesComparison dc, BlockingQueue<FileOperation> fileOperations) {
        super(localAddr, tasks, dc);
        this.fileOperations=fileOperations;
    }

    @Override
    public void handle(AbstractMsg msg) {
        if(msg.getType()== MessageType.FILE){
            LOG.debug("{}: Listener: FILE from {}", localAddr, msg.getSender());


            FileMsg fileMsg=(FileMsg)msg;
            byte[] fileContent=fileMsg.getFile();
            fileOperations.add(new FileOperation(OperationType.ENTRY_COPY_OR_CREATE, fileMsg.getRelativePath()));

            try(OutputStream out = new FileOutputStream(dc.getAbsolutePath(fileMsg.getRelativePath()))){

                /*if (!fileOperations.containsKey(msg.getRecipient()))
                    fileOperations.put(msg.getRecipient(), new ArrayList<>());
                fileOperations.get(fileOperations).add(new FileOperation(OperationType.ENTRY_CREATE, fileMsg.getRelativePath()));*/

                out.write(fileContent);
                dc.setCreationTime(dc.getAbsolutePath(fileMsg.getRelativePath()),fileMsg.getCreationTime());
                tasks.offer(new ResultMsg(localAddr, msg.getSender(), MessageState.SUCCESS, msg));

            }
            catch(IOException e){
                tasks.offer(new ResultMsg(localAddr, msg.getSender(), MessageState.FAILED, msg));

            }

        }
    }
}
