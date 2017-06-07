package ru.ifmo.diploma.synchronizer.exchange;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.ifmo.diploma.synchronizer.discovery.CurrentConnections;
import ru.ifmo.diploma.synchronizer.discovery.Discovery;
import ru.ifmo.diploma.synchronizer.Utils;
import ru.ifmo.diploma.synchronizer.messages.AbstractMsg;
import ru.ifmo.diploma.synchronizer.messages.SendListFilesMsg;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

/**
 * Created by ksenia on 29.05.2017.
 */
public class TWriter extends Thread {
    private static final Logger LOG = LogManager.getLogger(TWriter.class);

    private Discovery discovery;
    private Socket socket;
    private String addr;
    private CurrentConnections currentConnections;

    public TWriter(Discovery discovery, Socket socket, String addr) {
        this.discovery = discovery;
        this.socket = socket;
        this.addr = addr;
    }


    @Override
    public void run() {

        int localPort = discovery.getLocalPort();
        String localAddr = discovery.getLocalAddr();

        LOG.debug(localAddr + ": writer to " + addr + " started");

        BlockingQueue<AbstractMsg> tasks = discovery.getTasks();
        Map<String, CurrentConnections> connections = discovery.getConnections();
        CurrentConnections currentConnections = connections.get(addr);
        List<Socket> socketList = discovery.getSocketList();

        LOG.debug(localAddr + ": writer: Current connections: " + currentConnections + " with host " + addr);

//        ObjectInputStream objIn = currentConnections.getObjIn();
        ObjectOutputStream objOut = currentConnections.getObjOut();

        try {
            LOG.debug(localAddr + ": SendListFilesMsg to " + addr);
            objOut.writeObject(new SendListFilesMsg(localAddr));  //??? формируем (оборачиваем) и отправляем список файлов без запроса
            objOut.flush();

            LOG.debug(localAddr + ": ready for writing to " + addr);
            while (!isInterrupted()) {

                AbstractMsg msg = tasks.take();
                LOG.debug(localAddr + ": " + msg + " to " + addr);

                objOut.writeObject(msg);
                objOut.flush();


            }

        } catch (InterruptedException e) {
            LOG.debug(localAddr + ": Interrupted " + Arrays.toString(e.getStackTrace()));
            interrupt();
        } catch (IOException e) {
            LOG.error(localAddr + ": Writer error. Write object error");
            LOG.debug("writer: " + Arrays.toString(e.getStackTrace()));
        } finally {
            LOG.error(localAddr + ": Writer error. " + addr + " stopped");
            connections.remove(addr);
            socketList.remove(socket);
            Utils.closeSocket(socket);
        }
    }


}
