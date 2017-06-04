package ru.ifmo.diploma.synchronizer.messages;

import ru.ifmo.diploma.synchronizer.FileInfo;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/*
 * Created by Юлия on 30.05.2017.
 */
public class SendFileRequestMsg extends AbstractMsg implements Externalizable{
    private FileInfo fileInfo;

    public SendFileRequestMsg(){}

    public SendFileRequestMsg(String sender, FileInfo fileInfo){
        super(sender);
        super.type=MessageType.SEND_FILE;
        this.fileInfo=fileInfo;
    }

    public FileInfo getFileInfo(){
        return fileInfo;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(sender);
        out.writeObject(type);
        out.writeObject(fileInfo);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        sender=in.readObject().toString();
        type=(MessageType)in.readObject();
        fileInfo=(FileInfo)in.readObject();
    }
}
