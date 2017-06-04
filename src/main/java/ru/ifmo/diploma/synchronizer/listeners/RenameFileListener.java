package ru.ifmo.diploma.synchronizer.listeners;

import ru.ifmo.diploma.synchronizer.DirectoriesComparison;
import ru.ifmo.diploma.synchronizer.messages.*;

import java.io.File;
import java.util.concurrent.BlockingQueue;

/*
 * Created by Юлия on 04.06.2017.
 */
public class RenameFileListener extends AbstractListener {

    public RenameFileListener(String localAddr, BlockingQueue<AbstractMsg> tasks, DirectoriesComparison dc) {
        super(localAddr, tasks, dc);
    }

    @Override
    public void handle(AbstractMsg msg) {
        if(msg.getType()== MessageType.RENAME_FILE) {
            RenameFileMsg renameMsg=(RenameFileMsg)msg;
            File oldFile = new File(dc.getAbsolutePath(renameMsg.getOldRelativePath()));
            File newFile = new File(dc.getAbsolutePath(renameMsg.getNewRelativePath()));
            if (!oldFile.renameTo(newFile))
                //System.err.println("Renaming failed");
                tasks.offer(new ResultMsg(msg.getSender(), MessageState.FAILED, msg.getType()));
            else
                tasks.offer(new ResultMsg(msg.getSender(), MessageState.SUCCESS, msg.getType()));
        }
    }
}
