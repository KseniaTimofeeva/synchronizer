package ru.ifmo.diploma.synchronizer.messages;

import java.io.Serializable;

/**
 * Created by ksenia on 05.06.2017.
 */
public class FileMsgExample extends AbstractMsg implements Serializable {

    //??? инфа, какой файл в сообщении

    private byte[] bfile;

    public FileMsgExample(String sender, byte[] bfile) {
        super(sender);
        this.bfile = bfile;
    }

    public byte[] getBfile() {
        return bfile;
    }
}
