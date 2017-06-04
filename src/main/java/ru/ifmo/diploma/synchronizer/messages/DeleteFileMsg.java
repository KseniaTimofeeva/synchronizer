package ru.ifmo.diploma.synchronizer.messages;


import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/*
 * Created by Юлия on 01.06.2017.
 */
public class DeleteFileMsg extends AbstractMsg implements Externalizable{
    private String relativePath;

    public DeleteFileMsg(){}

    public DeleteFileMsg(String sender, String relativePath){
        super(sender);
        type= MessageType.DELETE_FILE;
        this.relativePath=relativePath;
    }

    public String getRelativePath(){
        return relativePath;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(sender);
        out.writeObject(type);
        out.writeObject(relativePath);

    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        sender=in.readObject().toString();
        type=(MessageType)in.readObject();
        relativePath=in.readObject().toString();
    }
}
