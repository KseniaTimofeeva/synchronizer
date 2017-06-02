package ru.ifmo.diploma.synchronizer.exchange;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.ifmo.diploma.synchronizer.discovery.CurrentConnections;
import ru.ifmo.diploma.synchronizer.discovery.Discovery;
import ru.ifmo.diploma.synchronizer.Utils;
import ru.ifmo.diploma.synchronizer.exchange.listeners.Listener;
import ru.ifmo.diploma.synchronizer.exchange.listeners.SendFileListListener;
import ru.ifmo.diploma.synchronizer.exchange.listeners.SendFileListener;
import ru.ifmo.diploma.synchronizer.protocol.exchange.AbstractMessage;
import ru.ifmo.diploma.synchronizer.protocol.exchange.SendFileListMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

/**
 * Created by ksenia on 29.05.2017.
 */
public class TWriter extends Thread {
    private static final Logger LOG = LogManager.getLogger(TWriter.class);

    private Discovery discovery;
    private Socket socket;
    private String addr;
    private CurrentConnections currentConnections;
    private int localPort;
    private String localAddr;

    public TWriter(Discovery discovery, Socket socket, String addr) {
        this.discovery = discovery;
        this.socket = socket;
        this.addr = addr;
    }


    @Override
    public void run() {

        localPort = discovery.getLocalPort();
        localAddr = discovery.getLocalAddr();

        LOG.debug(localAddr + ": writer to " + addr + " started");

        BlockingQueue<AbstractMessage> tasks = discovery.getTasks();
        Map<String, CurrentConnections> connections = discovery.getConnections();
        CurrentConnections currentConnections = connections.get(addr);

        LOG.debug(localAddr + ": writer: Current connections: " + currentConnections + " with host " + addr);

//        InputStream in = currentConnections.getIn();
        OutputStream out = currentConnections.getOut();
//        ObjectInputStream objIn = currentConnections.getObjIn();
        ObjectOutputStream objOut = currentConnections.getObjOut();

        try {
            LOG.debug(localAddr + ": SendFileListMsg to " + addr);
            objOut.writeObject(new SendFileListMessage(localAddr));  //запрашиваем список файлов
            objOut.flush();

            LOG.debug(localAddr + ": ready for writing to " + addr);
            while (!isInterrupted()) {

                AbstractMessage msg = tasks.take();
                //@TODO
                LOG.debug(localAddr + ": " + msg + " to " + addr);
                objOut.writeObject(msg);
                objOut.flush();
            }

        } catch (InterruptedException e) {
            LOG.debug(localAddr + ": Interrupted " + e.getStackTrace());
            interrupt();
        } catch (IOException e) {
            LOG.error(localAddr + ": Writer error. Write object error");
            LOG.error(e.getStackTrace());
        } finally {
            LOG.error(localAddr + ": Writer error. " + addr + " stopped");
            connections.remove(addr);
            Utils.closeSocket(socket);
        }
    }


}
