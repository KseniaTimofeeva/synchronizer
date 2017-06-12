package ru.ifmo.diploma.synchronizer.exchange;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.ifmo.diploma.synchronizer.Synchronizer;
import ru.ifmo.diploma.synchronizer.Utils;
import ru.ifmo.diploma.synchronizer.discovery.CurrentConnections;
import ru.ifmo.diploma.synchronizer.messages.AbstractMsg;
import ru.ifmo.diploma.synchronizer.messages.SendListFilesCommand;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

/**
 * Created by ksenia on 29.05.2017.
 */
public class TReader {
    private static final Logger LOG = LogManager.getLogger(TReader.class);

    private Thread currentThread;
    private Synchronizer synchronizer;
    private BlockingQueue<AbstractMsg> readerTasks;
    private Socket socket;
    private String addr;

    public TReader(Thread currentThread, Synchronizer synchronizer, BlockingQueue<AbstractMsg> readerTasks, Socket socket, String addr) {
        this.currentThread = currentThread;
        this.synchronizer = synchronizer;
        this.readerTasks = readerTasks;
        this.socket = socket;
        this.addr = addr;
    }

    public void startReader() {

        String localAddr = synchronizer.getLocalAddr();
        LOG.debug(localAddr + ": reader to " + addr + " started");

        Map<String, CurrentConnections> connections = synchronizer.getConnections();
        CurrentConnections currentConnections = connections.get(addr);

        LOG.trace(localAddr + ": reader: Current connections: " + currentConnections + " with host " + addr);

        ObjectInputStream objIn = currentConnections.getObjIn();

        try {
            //даем своему writer'у команду отправить список файлов удаленному хосту
            LOG.debug(localAddr + ": SEND_LIST_FILES_COMMAND to " + addr);
            readerTasks.offer(new SendListFilesCommand(localAddr, addr));

            //чтение сообщений от конкретного удаленного хоста
            LOG.trace(localAddr + ": ready for reading from " + addr);
            while (!currentThread.isInterrupted()) {

                Object obj = objIn.readObject();

                if (!(obj instanceof AbstractMsg)) {
                    LOG.error(localAddr + ": Unexpected message");
                    return;
                }

                readerTasks.offer((AbstractMsg) obj);
                LOG.trace("{}: reader: {} from {} added to queue", localAddr, ((AbstractMsg) obj).getType(), addr);
            }
        } catch (IOException | ClassNotFoundException e) {
            if (!Utils.exit) {
                LOG.error(localAddr + ": Reader error. Read object error");
            }
        } finally {
            if (!Utils.exit) {
                LOG.error(localAddr + ": Reader error. " + addr + " stopped");
            }
            connections.remove(addr);
            Utils.closeSocket(socket);
        }


    }
}
