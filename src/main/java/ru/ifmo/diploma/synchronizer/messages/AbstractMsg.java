package ru.ifmo.diploma.synchronizer.messages;

/*
 * Created by Юлия on 30.05.2017.
 */


public abstract class AbstractMsg {
    String sender;
    MessageType type;

    public AbstractMsg(){}

    public AbstractMsg(String sender){
        this.sender=sender;
    }

    public MessageType getType(){
        return type;
    }

    public String getSender(){
        return sender;
    }
}
