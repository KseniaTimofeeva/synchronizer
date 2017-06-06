package ru.ifmo.diploma.synchronizer.listeners;

import ru.ifmo.diploma.synchronizer.DirectoriesComparison;
import ru.ifmo.diploma.synchronizer.FileInfo;
import ru.ifmo.diploma.synchronizer.messages.*;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.concurrent.BlockingQueue;

/*
 * Created by Юлия on 04.06.2017.
 */
public class SendListFilesListener extends AbstractListener {
    List<FileInfo> sentListFiles;
    List<FileInfo> localListFiles;

    public SendListFilesListener(String localAddr, BlockingQueue<AbstractMsg> tasks, DirectoriesComparison dc, List<FileInfo> localListFiles) {
        super(localAddr, tasks, dc);
        this.localListFiles=localListFiles;
    }

    @Override
    public void handle(AbstractMsg msg) {

        if (msg.getType() == MessageType.SEND_LIST_FILES) {
                if(((SendListFilesMsg)msg).getListFiles()!=null) {
                    sentListFiles=((SendListFilesMsg)msg).getListFiles();
                    tasks.offer(new ResultMsg(msg.getSender(), MessageState.SUCCESS, msg));
                    dc.compareDirectories(sentListFiles, localListFiles);
                }
                else {
                    tasks.offer(new ResultMsg(msg.getSender(), MessageState.FAILED, msg));
                }

        }
    }
}
