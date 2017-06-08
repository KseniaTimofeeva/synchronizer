package ru.ifmo.diploma.synchronizer.messages;

/*
 * Created by Юлия on 30.05.2017.
 */


public abstract class AbstractMsg {
    String sender;
    private String recipient;
    MessageType type;
    private boolean broadcast;

    public AbstractMsg() {
    }

    //broadcast
    public AbstractMsg(String sender) {
        this.sender = sender;
        broadcast = true;
    }

    //unicast
    public AbstractMsg(String sender, String recipient) {
        this.sender = sender;
        this.recipient = recipient;
        broadcast = false;
    }

    public MessageType getType() {
        return type;
    }

    public String getSender() {
        return sender;
    }

    public String getRecipient() {
        return recipient;
    }

    public boolean isBroadcast() {
        return broadcast;
    }
}
