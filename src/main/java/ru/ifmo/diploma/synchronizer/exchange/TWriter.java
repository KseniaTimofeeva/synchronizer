package ru.ifmo.diploma.synchronizer.exchange;

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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 * Created by ksenia on 29.05.2017.
 */
public class TWriter extends Thread {

    private BlockingQueue<AbstractMessage> tasks;
    private InputStream in;
    private OutputStream out;
    private ObjectInputStream objIn;
    private ObjectOutputStream objOut;
    private int localPort;
    private String addr;
    private List<Listener<AbstractMessage>> listeners = new ArrayList<>();

    public TWriter(BlockingQueue<AbstractMessage> tasks, InputStream in, OutputStream out,
                   ObjectInputStream objIn, ObjectOutputStream objOut, int localPort, String addr) {
        this.tasks = tasks;
        this.in = in;
        this.out = out;
        this.objIn = objIn;
        this.objOut = objOut;
        this.localPort = localPort;
        this.addr = addr;
        addListeners();
    }

    private void addListeners() {
        listeners.add(new SendFileListener());
        listeners.add(new SendFileListListener());
    }

    @Override
    public void run() {
        System.out.println(localPort + ": writer to " + addr);

//        //запрашивает список файлов
//        try {
//            objOut.writeObject(new SendFileListMessage());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        while (!isInterrupted()) {
            try {

                AbstractMessage msg = tasks.take(); //нужно проверять на empty и в цикл?
                notifyListeners(msg);





            } catch (InterruptedException e) {
                e.printStackTrace();
                interrupt();
            }
        }
    }

    private void notifyListeners(AbstractMessage msg) {
        for (Listener listener : listeners) {
            listener.send(msg);
        }
    }
}
