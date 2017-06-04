package ru.ifmo.diploma.synchronizer.listeners;

import ru.ifmo.diploma.synchronizer.messages.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.BlockingQueue;

import ru.ifmo.diploma.synchronizer.DirectoriesComparison;

/*
 * Created by Юлия on 04.06.2017.
 */
public class CopyFileListener extends AbstractListener {

    public CopyFileListener(String localAddr, BlockingQueue<AbstractMsg> tasks, DirectoriesComparison dc) {
        super(localAddr, tasks, dc);
    }

    @Override
    public void handle(AbstractMsg msg) {
        if (msg.getType() == MessageType.COPY_FILE) {
            CopyFileMsg copyMsg = (CopyFileMsg) msg;
            Path oldPath = Paths.get(dc.getAbsolutePath(copyMsg.getRelativePath()));
            Path newPath = Paths.get(dc.getAbsolutePath(copyMsg.getNewRelativePath()));
            try {
                Files.copy(oldPath, newPath);
                dc.setCreationTime(newPath.toString(), copyMsg.getCreationTime());
                tasks.offer(new ResultMsg(msg.getSender(), MessageState.SUCCESS, MessageType.COPY_FILE));

            } catch (IOException e) {
                tasks.offer(new ResultMsg(msg.getSender(), MessageState.FAILED, MessageType.COPY_FILE));
                e.printStackTrace();
            }

        }
    }
}
