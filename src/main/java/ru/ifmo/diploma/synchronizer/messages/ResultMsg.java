package ru.ifmo.diploma.synchronizer.messages;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/*
 * Created by Юлия on 30.05.2017.
 */
public class ResultMsg extends AbstractMsg implements Externalizable{
    private MessageState state;
    private MessageType requestMsgType;

    public ResultMsg(){}

    public ResultMsg(String sender, MessageState state, MessageType requestMsgType){
        super(sender);
        super.type=MessageType.RESULT;
        this.state=state;
        this.requestMsgType =requestMsgType;
    }

    public MessageState getState(){
        return state;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(this.sender);
        out.writeObject(this.type);
        out.writeObject(state);
        out.writeObject(requestMsgType);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        sender=in.readObject().toString();
        type=(MessageType)in.readObject();
        state=(MessageState)in.readObject();
        requestMsgType =(MessageType)in.readObject();

    }
}
