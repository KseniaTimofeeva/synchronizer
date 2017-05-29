package ru.ifmo.diploma.synchronizer.protocol.handshake;

import java.io.Serializable;

/**
 * Created by ksenia on 25.05.2017.
 */
public abstract class HandshakeMessage implements Serializable {

    private String fromAddr;

    public HandshakeMessage(String fromAddr) {
        this.fromAddr = fromAddr;
    }

    public String getFromAddr() {
        return fromAddr;
    }
}
