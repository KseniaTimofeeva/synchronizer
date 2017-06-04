package ru.ifmo.diploma.synchronizer.listeners;

import ru.ifmo.diploma.synchronizer.messages.AbstractMsg;

/**
 * Created by Юлия on 04.06.2017.
 */
public interface Listener <T extends AbstractMsg>{

    public void handle(T msg);
}
