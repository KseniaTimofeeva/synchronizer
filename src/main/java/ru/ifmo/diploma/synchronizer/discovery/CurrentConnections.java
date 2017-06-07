package ru.ifmo.diploma.synchronizer.discovery;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

/**
 * Created by ksenia on 30.05.2017.
 */
public class CurrentConnections {
    private ObjectInputStream objIn;
    private ObjectOutputStream objOut;

    public CurrentConnections(ObjectInputStream objIn, ObjectOutputStream objOut) {
        this.objIn = objIn;
        this.objOut = objOut;
    }

    public ObjectInputStream getObjIn() {
        return objIn;
    }

    public ObjectOutputStream getObjOut() {
        return objOut;
    }

    @Override
    public String toString() {
        return "CurrentConnections{" +
                "objIn=" + objIn +
                ", objOut=" + objOut +
                '}';
    }
}
