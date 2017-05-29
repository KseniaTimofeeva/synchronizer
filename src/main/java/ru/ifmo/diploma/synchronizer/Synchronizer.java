package ru.ifmo.diploma.synchronizer;

import ru.ifmo.diploma.synchronizer.discovery.Discovery;
import ru.ifmo.diploma.synchronizer.protocol.Credentials;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ksenia on 20.05.2017.
 */
public class Synchronizer {
    public static void main(String[] args) {
        Map<String, Credentials> authorizationTable1 = new HashMap<>();
        authorizationTable1.put("127.0.0.1:60601", new Credentials("","login60601", "password60601"));
        authorizationTable1.put("127.0.0.1:60602", new Credentials("","login60602", "password60602"));
        authorizationTable1.put("127.0.0.1:60603", new Credentials("","login60603", "password60603"));

        Map<String, Credentials> authorizationTable2 = new HashMap<>();
        authorizationTable2.put("127.0.0.1:60601", new Credentials("","login60601", "password60601"));
        authorizationTable2.put("127.0.0.1:60603", new Credentials("","login60603", "password60603"));

        Map<String, Credentials> authorizationTable3 = new HashMap<>();
        authorizationTable3.put("127.0.0.1:60602", new Credentials("","login60602", "password60602"));
        authorizationTable3.put("127.0.0.1:60604", new Credentials("","login60604", "password60604"));

        Map<String, Credentials> authorizationTable4 = new HashMap<>();
        authorizationTable4.put("127.0.0.1:60603", new Credentials("","login60603", "password60603"));


        new Discovery(60601, authorizationTable1).startDiscovery();
        threadSleep(1000);
        new Discovery(60602, authorizationTable2).startDiscovery();
        threadSleep(1000);
        new Discovery(60603, authorizationTable3).startDiscovery();
        threadSleep(1000);
        new Discovery(60604, authorizationTable4).startDiscovery();

    }

    private static void threadSleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
