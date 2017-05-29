package ru.ifmo.diploma.synchronizer.exchange.listeners;

import ru.ifmo.diploma.synchronizer.protocol.exchange.AbstractMessage;

/**
 * Created by ksenia on 29.05.2017.
 */
public interface  Listener <T extends AbstractMessage> {
    void send(T msg);
}
