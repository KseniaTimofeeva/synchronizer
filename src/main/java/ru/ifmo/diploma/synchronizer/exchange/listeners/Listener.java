package ru.ifmo.diploma.synchronizer.exchange.listeners;

import ru.ifmo.diploma.synchronizer.discovery.CurrentConnections;
import ru.ifmo.diploma.synchronizer.protocol.exchange.AbstractMessage;

import java.io.IOException;

/**
 * Created by ksenia on 29.05.2017.
 */
public interface Listener<T extends AbstractMessage> {

    void handle(T msg, CurrentConnections currentConnections);
}
