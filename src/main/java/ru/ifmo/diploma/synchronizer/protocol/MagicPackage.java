package ru.ifmo.diploma.synchronizer.protocol;

import java.io.Serializable;

/**
 * Created by ksenia on 22.05.2017.
 */
public class MagicPackage implements Serializable {
    private int from;

    public MagicPackage(int from) {
        this.from = from;
    }

    public int getFrom() {
        return from;
    }
}
