package ru.ifmo.diploma.synchronizer.listeners;

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

    public DeleteFileListener(String localAddr, BlockingQueue<AbstractMsg> tasks, DirectoriesComparison dc) {
        super(localAddr, tasks, dc);
    }

    @Override
    public void handle(AbstractMsg msg) {
        if(msg.getType()== MessageType.DELETE_FILE) {
            DeleteFileMsg delMsg=(DeleteFileMsg)msg;
            try {
                Files.deleteIfExists(Paths.get(dc.getAbsolutePath(delMsg.getRelativePath())));
                tasks.offer(new ResultMsg(msg.getSender(), MessageState.SUCCESS, msg));

            } catch (IOException e) {
                tasks.offer(new ResultMsg(msg.getSender(), MessageState.FAILED, msg));
                e.printStackTrace();
            }
        }

    }
}
