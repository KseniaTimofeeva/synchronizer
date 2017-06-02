package ru.ifmo.diploma.synchronizer.exchange.listeners;

import ru.ifmo.diploma.synchronizer.discovery.CurrentConnections;
import ru.ifmo.diploma.synchronizer.protocol.exchange.AbstractMessage;

import java.util.concurrent.BlockingQueue;

/**
 * Created by ksenia on 29.05.2017.
 */
public class SendFileListener implements Listener<AbstractMessage> {
    private String localAddr;
    private BlockingQueue<AbstractMessage> tasks;

    public SendFileListener(String localAddr, BlockingQueue<AbstractMessage> tasks) {
        this.localAddr = localAddr;
        this.tasks = tasks;
    }

    @Override
    public void handle(AbstractMessage msg, CurrentConnections currentConnections) {

    }
}
