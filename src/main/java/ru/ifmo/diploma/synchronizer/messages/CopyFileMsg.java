package ru.ifmo.diploma.synchronizer.messages;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/*
 * Created by Юлия on 30.05.2017.
 */
public class CopyFileMsg extends AbstractMsg implements Externalizable {
    private String relativePath;
    private String newRelativePath;
    private long creationTime;

    public CopyFileMsg() {
    }

    //broadcast
    public CopyFileMsg(String sender, String relativePath, String newRelativePath, long creationTime) {
        super(sender);
        super.type = MessageType.COPY_FILE;
        this.relativePath = relativePath;
        this.newRelativePath = newRelativePath;
        this.creationTime = creationTime;
    }

    //unicast
    public CopyFileMsg(String sender, String recipient, String relativePath, String newRelativePath, long creationTime) {
        super(sender, recipient);
        super.type = MessageType.COPY_FILE;
        this.relativePath = relativePath;
        this.newRelativePath = newRelativePath;
        this.creationTime = creationTime;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public String getNewRelativePath() {
        return newRelativePath;
    }

    public long getCreationTime() {
        return creationTime;
    }


    @Override
    public void writeExternal(ObjectOutput out) throws IOException {

        out.writeObject(sender);
        out.writeObject(type);
        out.writeObject(relativePath);
        out.writeObject(newRelativePath);
        out.writeLong(creationTime);

    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        sender = in.readObject().toString();
        type = (MessageType) in.readObject();
        relativePath = in.readObject().toString();
        newRelativePath = in.readObject().toString();
        creationTime = in.readLong();

    }
}
