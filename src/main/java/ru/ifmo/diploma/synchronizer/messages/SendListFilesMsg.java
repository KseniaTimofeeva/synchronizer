package ru.ifmo.diploma.synchronizer.messages;

import ru.ifmo.diploma.synchronizer.FileInfo;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.List;

/*
 * Created by Юлия on 30.05.2017.
 */
public class SendListFilesMsg extends AbstractMsg implements Externalizable{
    private List<FileInfo> listFiles;

    public SendListFilesMsg(){}

    public SendListFilesMsg(String sender, List<FileInfo> listFiles){
        super(sender);
        super.type=MessageType.SEND_LIST_FILES;
        this.listFiles=listFiles;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(sender);
        out.writeObject(type);
        out.writeObject(listFiles);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        sender=in.readObject().toString();
        type=(MessageType)in.readObject();
        listFiles=(List<FileInfo>)in.readObject();
    }

    public List<FileInfo> getListFiles(){
        return listFiles;
    }
}
