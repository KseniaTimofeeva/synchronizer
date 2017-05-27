package ru.ifmo.diploma.synchronizer.protocol;

/**
 * Created by ksenia on 24.05.2017.
 */
public class YesNoPackage extends HandshakeMessage {
    private boolean status;
    private String cause;

    public YesNoPackage(String fromAddr, boolean status, String cause) {
        super(fromAddr);
        this.status = status;
        this.cause = cause;
    }

    public boolean getStatus() {
        return status;
    }

    public String getCause() {
        return cause;
    }
}
