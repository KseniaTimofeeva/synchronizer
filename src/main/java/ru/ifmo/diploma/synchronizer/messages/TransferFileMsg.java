package ru.ifmo.diploma.synchronizer.messages;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/*
 * Created by Юлия on 30.05.2017.
 */
public class TransferFileMsg extends AbstractMsg implements Externalizable{
    private String oldRelativePath;
    private String newRelativePath;

    public TransferFileMsg(){}

    public TransferFileMsg(String sender, String relativePath, String newRelativePath){
        super(sender);
        super.type=MessageType.TRANSFER_FILE;
        this.oldRelativePath=relativePath;
        this.newRelativePath=newRelativePath;
    }

    public String getOldRelativePath(){
        return oldRelativePath;
    }

    public String getNewRelativePath(){
        return newRelativePath;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(sender);
        out.writeObject(type);
        out.writeObject(oldRelativePath);
        out.writeObject(newRelativePath);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        sender=in.readObject().toString();
        type=(MessageType)in.readObject();
        oldRelativePath=in.readObject().toString();
        newRelativePath=in.readObject().toString();
    }
}
