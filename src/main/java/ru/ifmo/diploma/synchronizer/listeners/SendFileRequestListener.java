package ru.ifmo.diploma.synchronizer.listeners;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.ifmo.diploma.synchronizer.DirectoriesComparison;
import ru.ifmo.diploma.synchronizer.FileInfo;
import ru.ifmo.diploma.synchronizer.messages.*;

import java.io.*;
import java.util.concurrent.BlockingQueue;

/*
 * Created by Юлия on 04.06.2017.
 */
public class SendFileRequestListener extends AbstractListener {
    private static final Logger LOG = LogManager.getLogger(SendFileRequestListener.class);

    public SendFileRequestListener(String localAddr, BlockingQueue<AbstractMsg> tasks, DirectoriesComparison dc) {
        super(localAddr, tasks, dc);
    }

    @Override
    public void handle(AbstractMsg msg) {
        if (msg.getType() == MessageType.SEND_FILE_REQUEST) {
            LOG.debug("{}: Listener: SEND_FILE_REQUEST from {}", localAddr, msg.getSender());

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
                tasks.offer(new FileMsg(msg.getSender(), bout.toByteArray(), path));

            } catch (IOException e) {
                tasks.offer(new ResultMsg(localAddr, msg.getSender(), MessageState.FAILED, msg));
                return;
            }
            tasks.offer(new ResultMsg(localAddr, msg.getSender(), MessageState.SUCCESS, msg));

        }
    }
}
