package ru.ifmo.diploma.synchronizer.messages;

import sun.plugin2.message.Message;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/*
 * Created by Юлия on 06.06.2017.
 */
public class FileMsg extends AbstractMsg implements Externalizable{
    private byte[] file;
    private String relativePath;
    private long creationTime;

    public FileMsg(){}

    //broadcast
    public FileMsg(String sender, byte[] file, String relativePath, long creationTime){
        super(sender);
        type=MessageType.FILE;
        this.file=file;
        this.relativePath=relativePath;
        this.creationTime=creationTime;
    }

    //unicast
    public FileMsg(String sender, String recipient, byte[] file, String relativePath){
        super(sender, recipient);
        type=MessageType.FILE;
        this.file=file;
        this.relativePath=relativePath;
    }

    public byte[] getFile(){ return file; }

    public long getCreationTime(){ return creationTime; }

    public String getRelativePath(){ return relativePath; }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(sender);
        out.writeObject(type);
        out.writeObject(file);
        out.writeObject(relativePath);
        out.writeLong(creationTime);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        sender=in.readObject().toString();
        type=(MessageType)in.readObject();
        file=(byte[])in.readObject();
        relativePath=in.readObject().toString();
        creationTime=in.readLong();
    }
}
