package ru.ifmo.diploma.synchronizer.exchange.listeners;

import ru.ifmo.diploma.synchronizer.protocol.exchange.AbstractMessage;
import ru.ifmo.diploma.synchronizer.protocol.exchange.SendFileListMessage;

/**
 * Created by ksenia on 29.05.2017.
 */
public class SendFileListListener implements Listener<AbstractMessage> {

    @Override
    public void send(AbstractMessage msg) {
        if (msg instanceof SendFileListMessage) {


        }
    }
}
