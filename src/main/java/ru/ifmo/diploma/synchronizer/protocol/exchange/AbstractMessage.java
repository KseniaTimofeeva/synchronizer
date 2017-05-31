package ru.ifmo.diploma.synchronizer.protocol.exchange;

/**
 * Created by ksenia on 28.05.2017.
 */
public abstract class AbstractMessage {
    private String from;

    public AbstractMessage(String from) {
        this.from = from;
    }

    public String getFrom() {
        return from;
    }
}
