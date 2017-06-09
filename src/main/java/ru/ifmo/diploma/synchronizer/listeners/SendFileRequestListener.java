package ru.ifmo.diploma.synchronizer.listeners;

import ru.ifmo.diploma.synchronizer.DirectoriesComparison;
import ru.ifmo.diploma.synchronizer.FileInfo;
import ru.ifmo.diploma.synchronizer.messages.*;

import java.io.*;
import java.util.concurrent.BlockingQueue;

/*
 * Created by Юлия on 04.06.2017.
 */
public class SendFileRequestListener extends AbstractListener {

    public SendFileRequestListener(String localAddr, BlockingQueue<AbstractMsg> tasks, DirectoriesComparison dc) {
        super(localAddr, tasks, dc);
    }

    @Override
    public void handle(AbstractMsg msg) {
        if (msg.getType() == MessageType.SEND_FILE_REQUEST) {
            SendFileRequestMsg fileRequestMsg = (SendFileRequestMsg) msg;
            FileInfo fi = fileRequestMsg.getFileInfo();
            File f = new File(dc.getAbsolutePath(fi.getRelativePath()));
            try (InputStream in = new FileInputStream(f);
                 ByteArrayOutputStream bout = new ByteArrayOutputStream()) {

                int l;
                byte[] buf = new byte[1024];
                while ((l = in.read(buf)) > 0) {
                    bout.write(buf, 0, l);
                }
                String path = fi.getRelativePath();
                if (fileRequestMsg.getChangeName()) {
                    StringBuilder sbPath = new StringBuilder();
                    sbPath.append(path.substring(0, path.length() - 4));
                    sbPath.append(msg.getSender());
                    sbPath.append(path.substring(path.indexOf('.')));
                    path = sbPath.toString();
                }
                tasks.offer(new FileMsg(msg.getSender(), bout.toByteArray(), path, fi.getCreationDate()));

            } catch (IOException e) {
                tasks.offer(new ResultMsg(msg.getSender(), MessageState.FAILED, msg));
                return;
            }
            tasks.offer(new ResultMsg(msg.getSender(), MessageState.SUCCESS, msg));

        }
    }
}
