package ru.ifmo.diploma.synchronizer.protocol;

import java.io.Serializable;

/**
 * Created by ksenia on 24.05.2017.
 */
public class HelloPackage implements Serializable {
    private String from;

    public HelloPackage(String from) {
        this.from = from;
    }
}
