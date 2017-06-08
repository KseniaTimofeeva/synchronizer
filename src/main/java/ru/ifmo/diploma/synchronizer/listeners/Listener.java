package ru.ifmo.diploma.synchronizer.listeners;

import ru.ifmo.diploma.synchronizer.messages.AbstractMsg;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

/**
 * Created by Юлия on 04.06.2017.
 */
public interface Listener <T extends AbstractMsg>{

    void handle(T msg) throws IOException, NoSuchAlgorithmException;
}
