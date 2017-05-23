package ru.ifmo.diploma.synchronizer.protocol;

import java.io.Serializable;

/**
 * Created by ksenia on 24.05.2017.
 */
public class YesNoPackage implements Serializable {
    private boolean status;

    public YesNoPackage(boolean status) {
        this.status = status;
    }
}
