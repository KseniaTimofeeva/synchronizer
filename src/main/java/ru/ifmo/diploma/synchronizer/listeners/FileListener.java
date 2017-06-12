package ru.ifmo.diploma.synchronizer.listeners;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.ifmo.diploma.synchronizer.DirectoriesComparison;
import ru.ifmo.diploma.synchronizer.messages.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;

/*
 * Created by Юлия on 06.06.2017.
 */
public class FileListener extends AbstractListener {
    private static final Logger LOG = LogManager.getLogger(FileListener.class);

    public FileListener(String localAddr, BlockingQueue<AbstractMsg> tasks, DirectoriesComparison dc) {
        super(localAddr, tasks, dc);
    }

    @Override
    public void handle(AbstractMsg msg) {
        if (msg.getType() == MessageType.FILE) {
            LOG.debug("{}: Listener: FILE from {}", localAddr, msg.getSender());

            FileMsg fileMsg = (FileMsg) msg;
            byte[] fileContent = fileMsg.getFile();

            try (OutputStream out = new FileOutputStream(dc.getAbsolutePath(fileMsg.getRelativePath()))) {
                out.write(fileContent);
                dc.setCreationTime(dc.getAbsolutePath(fileMsg.getRelativePath()), fileMsg.getCreationTime());
                if (!msg.isBroadcast()) {
                    tasks.offer(new ResultMsg(localAddr, msg.getSender(), MessageState.SUCCESS, msg));
                }
            } catch (IOException e) {
                if (!msg.isBroadcast()) {
                    tasks.offer(new ResultMsg(localAddr, msg.getSender(), MessageState.FAILED, msg));
                }
            }
        }
    }
}
