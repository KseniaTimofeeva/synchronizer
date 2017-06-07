package ru.ifmo.diploma.synchronizer.messages;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/*
 * Created by Юлия on 07.06.2017.
 */
public class FinishMsg extends AbstractMsg implements Externalizable{

    public  FinishMsg(){}

    public FinishMsg(String sender){
        super(sender);
        type=MessageType.FINISH;
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
