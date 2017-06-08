package ru.ifmo.diploma.synchronizer.messages;

import java.io.Serializable;

/**
 * Created by ksenia on 08.06.2017.
 */
public class SendListFilesCommand extends AbstractMsg implements Serializable {

    //unicast
    public SendListFilesCommand(String sender, String recipient) {
        super(sender, recipient);
        type = MessageType.SEND_LIST_FILES_COMMAND;
    }
}
