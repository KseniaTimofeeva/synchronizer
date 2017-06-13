package ru.ifmo.diploma.synchronizer.discovery;

import ru.ifmo.diploma.synchronizer.exchange.TWriter;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Created by ksenia on 30.05.2017.
 */
public class CurrentConnections {
    private Thread thread;
    private Socket socket;
    private ObjectInputStream objIn;
    private ObjectOutputStream objOut;
//    private TWriter writer;

    public CurrentConnections(Thread thread, Socket socket) {
        this.thread = thread;
        this.socket = socket;
    }

    public CurrentConnections(Thread thread, Socket socket, ObjectInputStream objIn, ObjectOutputStream objOut) {
        this.thread = thread;
        this.socket = socket;
        this.objIn = objIn;
        this.objOut = objOut;
    }

    public void setObjInObjOut(ObjectInputStream objIn, ObjectOutputStream objOut) {
        this.objIn = objIn;
        this.objOut = objOut;
    }

    public ObjectInputStream getObjIn() {
        return objIn;
    }

    public ObjectOutputStream getObjOut() {
        return objOut;
    }

    public Socket getSocket() {
        return socket;
    }

    public Thread getThread() {
        return thread;
    }

    @Override
    public String toString() {
        return "CurrentConnections{" +
                "objIn=" + objIn +
                ", objOut=" + objOut +
                '}';
    }
}
