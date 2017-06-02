package ru.ifmo.diploma.synchronizer.protocol.exchange;

import java.io.Serializable;

/**
 * Created by ksenia on 28.05.2017.
 */
public abstract class AbstractMessage implements Serializable {
    private String from;

    public AbstractMessage(String from) {
        this.from = from;
    }

    public String getFrom() {
        return from;
    }
}
