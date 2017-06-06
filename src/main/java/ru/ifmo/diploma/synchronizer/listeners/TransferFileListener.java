package ru.ifmo.diploma.synchronizer.listeners;

import ru.ifmo.diploma.synchronizer.DirectoriesComparison;
import ru.ifmo.diploma.synchronizer.messages.*;

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

    public TransferFileListener(String localAddr, BlockingQueue<AbstractMsg> tasks, DirectoriesComparison dc) {
        super(localAddr, tasks, dc);
    }

    @Override
    public void handle(AbstractMsg msg) {
        if(msg.getType()== MessageType.TRANSFER_FILE){

            TransferFileMsg transMsg=(TransferFileMsg)msg;
            Path oldPath = Paths.get(dc.getAbsolutePath(transMsg.getOldRelativePath()));
            String p=dc.getAbsolutePath(transMsg.getNewRelativePath());
            Path newPath = Paths.get(p);
            Path newDirPath=Paths.get(p.substring(0,p.lastIndexOf("\\")));

            if(!Files.exists(newDirPath)) {
                try {
                    Files.createDirectories(newDirPath);
                    Files.move(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING);
                    tasks.offer(new ResultMsg(localAddr, MessageState.SUCCESS, msg));

                } catch (IOException e) {
                    tasks.offer(new ResultMsg(localAddr, MessageState.FAILED, msg));
                    e.printStackTrace();
                }
            }
        }
    }
}
