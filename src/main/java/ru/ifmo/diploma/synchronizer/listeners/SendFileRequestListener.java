package ru.ifmo.diploma.synchronizer.listeners;

import ru.ifmo.diploma.synchronizer.DirectoriesComparison;
import ru.ifmo.diploma.synchronizer.FileInfo;
import ru.ifmo.diploma.synchronizer.messages.*;

import java.io.*;
import java.util.concurrent.BlockingQueue;

/**
 * Created by Юлия on 04.06.2017.
 */
public class SendFileRequestListener extends AbstractListener{

    ObjectOutputStream out;

    public SendFileRequestListener(String localAddr, BlockingQueue<AbstractMsg> tasks, DirectoriesComparison dc, ObjectOutputStream out) {
        super(localAddr, tasks, dc);
        this.out = out;
    }

    @Override
    public void handle(AbstractMsg msg) {
        if(msg.getType()== MessageType.SEND_FILE){
            FileInfo fi=((SendFileRequestMsg)msg).getFileInfo();
            File f=new File(dc.getAbsolutePath(fi.getRelativePath()));
            try (InputStream in = new FileInputStream(f);
                 ByteArrayOutputStream bout = new ByteArrayOutputStream()) {

                int l;
                byte[] buf = new byte[1024];
                while ((l = in.read(buf)) > 0) {
                    bout.write(buf, 0, l);
                }
                out.write(bout.toByteArray());
                out.flush();

            } catch (IOException e) {
                tasks.offer(new ResultMsg(msg.getSender(), MessageState.FAILED, msg.getType()));
                return;
            }
            tasks.offer(new ResultMsg(msg.getSender(), MessageState.SUCCESS, msg.getType()));

        }
    }
}
