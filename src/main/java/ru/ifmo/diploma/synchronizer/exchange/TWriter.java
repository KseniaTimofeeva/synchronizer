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

/**
 * Created by ksenia on 29.05.2017.
 */
public class TWriter extends Thread {
    private static final Logger LOG = LogManager.getLogger(TWriter.class);

    private Discovery discovery;
    private Socket socket;
    private String addr;
    private CurrentConnections currentConnections;
    private List<Listener<AbstractMessage>> listeners = new ArrayList<>();
    private int localPort;
    private String localAddr;

    public TWriter(Discovery discovery, Socket socket, String addr) {
        this.discovery = discovery;
        this.socket = socket;
        this.addr = addr;
        addListeners();
    }

    private void addListeners() {
        listeners.add(new SendFileListener());
        listeners.add(new SendFileListListener());
    }

    @Override
    public void run() {
        BlockingQueue<AbstractMessage> tasks = discovery.getTasks();
        localPort = discovery.getLocalPort();
        localAddr = discovery.getLocalAddr();
        Map<String, CurrentConnections> connections = discovery.getConnections();
        CurrentConnections currentConnections = connections.get(addr);

//        System.out.println(localPort + ": writer to " + addr);
        LOG.debug(localPort + ": writer to " + addr);

        InputStream in = currentConnections.getIn();
        OutputStream out = currentConnections.getOut();
        ObjectInputStream objIn = currentConnections.getObjIn();
        ObjectOutputStream objOut = currentConnections.getObjOut();

        try {
//            objOut.writeObject(new SendFileListMessage(localAddr));  //сообщение о готовности отправить список файлов


            while (!isInterrupted()) {

                AbstractMessage msg = tasks.take();
                notifyListeners(msg);

            }

        } catch (InterruptedException e) {
            LOG.debug(e.getStackTrace());
            interrupt();
        } catch (IOException e) {
            LOG.debug(e.getStackTrace());
        } finally {
            LOG.error(localPort + ": Writer error. " + addr + " stopped");
            connections.remove(addr);
            Utils.closeSocket(socket);
        }
    }

    private void notifyListeners(AbstractMessage msg) throws IOException {
        for (Listener<AbstractMessage> listener : listeners) {
            listener.handle(msg, currentConnections, localAddr);   //@TODO add inputstream
        }
    }
}
