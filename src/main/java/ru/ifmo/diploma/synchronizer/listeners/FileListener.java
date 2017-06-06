package ru.ifmo.diploma.synchronizer.listeners;

import ru.ifmo.diploma.synchronizer.DirectoriesComparison;
import ru.ifmo.diploma.synchronizer.messages.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;

/*
 * Created by Юлия on 06.06.2017.
 */
public class FileListener extends AbstractListener{

    public FileListener(String localAddr, BlockingQueue<AbstractMsg> tasks, DirectoriesComparison dc){
        super(localAddr, tasks, dc);
    }

    @Override
    public void handle(AbstractMsg msg) {
        if(msg.getType()== MessageType.FILE){
            FileMsg fileMsg=(FileMsg)msg;
            byte[] fileContent=fileMsg.getFile();

            try(OutputStream out = new FileOutputStream(dc.getAbsolutePath(fileMsg.getRelativePath()))){
                out.write(fileContent);
                tasks.offer(new ResultMsg(msg.getSender(), MessageState.SUCCESS, msg));

            }
            catch(IOException e){
                tasks.offer(new ResultMsg(msg.getSender(), MessageState.FAILED, msg));

            }

        }
    }
}
