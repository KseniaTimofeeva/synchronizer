package ru.ifmo.diploma.synchronizer.exchange;

import ru.ifmo.diploma.synchronizer.protocol.AbstractMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;

/**
 * Created by ksenia on 29.05.2017.
 */
public class TReader extends Thread {

    private BlockingQueue<AbstractMessage> tasks;
    private InputStream in;
    private OutputStream out;
    private ObjectInputStream objIn;
    private ObjectOutputStream objOut;
    private int localPort;
    private String addr;

    public TReader(BlockingQueue<AbstractMessage> tasks, InputStream in, OutputStream out,
                   ObjectInputStream objIn, ObjectOutputStream objOut, int localPort, String addr) {
        this.tasks = tasks;
        this.in = in;
        this.out = out;
        this.objIn = objIn;
        this.objOut = objOut;
        this.localPort = localPort;
        this.addr = addr;
    }

    @Override
    public void run() {
        System.out.println(localPort + ": reader to " + addr);

        while (!isInterrupted()) {
            try {

                Object obj = objIn.readObject();

                if (obj instanceof AbstractMessage) {

                }

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }


        }
    }
}
