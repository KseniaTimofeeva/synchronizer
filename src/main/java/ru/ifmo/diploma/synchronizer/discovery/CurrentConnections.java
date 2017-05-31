package ru.ifmo.diploma.synchronizer.discovery;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

/**
 * Created by ksenia on 30.05.2017.
 */
public class CurrentConnections {
    private InputStream in;
    private OutputStream out;
    private ObjectInputStream objIn;
    private ObjectOutputStream objOut;

    public CurrentConnections(InputStream in, OutputStream out, ObjectInputStream objIn, ObjectOutputStream objOut) {
        this.in = in;
        this.out = out;
        this.objIn = objIn;
        this.objOut = objOut;
    }

    public InputStream getIn() {
        return in;
    }

    public OutputStream getOut() {
        return out;
    }

    public ObjectInputStream getObjIn() {
        return objIn;
    }

    public ObjectOutputStream getObjOut() {
        return objOut;
    }
}
