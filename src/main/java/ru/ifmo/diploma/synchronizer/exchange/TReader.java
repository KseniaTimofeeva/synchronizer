package ru.ifmo.diploma.synchronizer.exchange;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.ifmo.diploma.synchronizer.Utils;
import ru.ifmo.diploma.synchronizer.discovery.CurrentConnections;
import ru.ifmo.diploma.synchronizer.discovery.Discovery;
import ru.ifmo.diploma.synchronizer.protocol.exchange.AbstractMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

/**
 * Created by ksenia on 29.05.2017.
 */
public class TReader extends Thread {
    private static final Logger LOG = LogManager.getLogger(TReader.class);

    private Discovery discovery;
    private Socket socket;
    private String addr;

    public TReader(Discovery discovery, Socket socket, String addr) {
        this.discovery = discovery;
        this.socket = socket;
        this.addr = addr;
    }

    @Override
    public void run() {
        BlockingQueue<AbstractMessage> tasks = discovery.getTasks();
        int localPort = discovery.getLocalPort();
        String localAddr = discovery.getLocalAddr();
        Map<String, CurrentConnections> connections = discovery.getConnections();
        CurrentConnections currentConnections = connections.get(addr);

//        System.out.println(localPort + ": reader to " + addr);
        LOG.debug(localPort + ": reader to " + addr);

        InputStream in = currentConnections.getIn();
        OutputStream out = currentConnections.getOut();
        ObjectInputStream objIn = currentConnections.getObjIn();
        ObjectOutputStream objOut = currentConnections.getObjOut();

        while (!isInterrupted()) {
            try {

                Object obj = objIn.readObject();

                if (obj instanceof AbstractMessage) {
                    tasks.offer((AbstractMessage) obj);
                }

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                return;
            } finally {
                LOG.error(localPort + ": Reader error. " + addr + " stopped");
                connections.remove(addr);
                Utils.closeSocket(socket);
            }


        }
    }
}
