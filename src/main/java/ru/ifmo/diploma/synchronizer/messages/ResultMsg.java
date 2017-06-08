package ru.ifmo.diploma.synchronizer.messages;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/*
 * Created by Юлия on 30.05.2017.
 */
public class ResultMsg extends AbstractMsg implements Externalizable {
    private MessageState state;
    private AbstractMsg requestMsg;

    public ResultMsg() {
    }

    //unicast
    public ResultMsg(String sender, String recipient, MessageState state, AbstractMsg requestMsg) {
        super(sender, recipient);
        super.type = MessageType.RESULT;
        this.state = state;
        this.requestMsg = requestMsg;
    }

    public MessageState getState() {
        return state;
    }

    public AbstractMsg getRequestMsg() { return requestMsg; }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(this.sender);
        out.writeObject(this.type);
        out.writeObject(state);
        out.writeObject(requestMsg);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        sender = in.readObject().toString();
        type = (MessageType) in.readObject();
        state = (MessageState) in.readObject();
        requestMsg = (AbstractMsg) in.readObject();

    }
}
