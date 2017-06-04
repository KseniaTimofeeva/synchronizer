package ru.ifmo.diploma.synchronizer.messages;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Created by Юлия on 30.05.2017.
 */
public class SendListFilesMsg extends AbstractMsg implements Externalizable{
    public SendListFilesMsg(){}

    public SendListFilesMsg(String sender){
        super(sender);
        super.type=MessageType.SEND_LIST_FILES;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(sender);
        out.writeObject(type);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        sender=in.readObject().toString();
        type=(MessageType)in.readObject();
    }
}
