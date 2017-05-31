package ru.ifmo.diploma.synchronizer.exchange.listeners;

import ru.ifmo.diploma.synchronizer.discovery.CurrentConnections;
import ru.ifmo.diploma.synchronizer.protocol.exchange.AbstractMessage;

/**
 * Created by ksenia on 29.05.2017.
 */
public class SendFileListener implements Listener<AbstractMessage> {

    @Override
    public void handle(AbstractMessage msg, CurrentConnections currentConnections, String localAddr) {

    }
}
