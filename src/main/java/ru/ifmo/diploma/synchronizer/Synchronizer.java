package ru.ifmo.diploma.synchronizer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.ifmo.diploma.synchronizer.discovery.Discovery;
import ru.ifmo.diploma.synchronizer.protocol.handshake.Credentials;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ksenia on 20.05.2017.
 */
public class Synchronizer {
    private static final Logger LOG = LogManager.getLogger(Synchronizer.class);

    public static void main(String[] args) {
        String localAddr = null;
        try {
            localAddr = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        LOG.debug("Local address: " + localAddr);

        Map<String, Credentials> authorizationTable1 = new HashMap<>();
        authorizationTable1.put(localAddr + ":60601", new Credentials("","login60601", "password60601"));
        authorizationTable1.put(localAddr + ":60602", new Credentials("","login60602", "password60602"));
        authorizationTable1.put(localAddr + ":60603", new Credentials("","login60603", "password60603"));

        Map<String, Credentials> authorizationTable2 = new HashMap<>();
        authorizationTable2.put(localAddr + ":60601", new Credentials("","login60601", "password60601"));
        authorizationTable2.put(localAddr + ":60603", new Credentials("","login60603", "password60603"));

        Map<String, Credentials> authorizationTable3 = new HashMap<>();
        authorizationTable3.put(localAddr + ":60602", new Credentials("","login60602", "password60602"));
        authorizationTable3.put(localAddr + ":60604", new Credentials("","login60604", "password60604"));

        Map<String, Credentials> authorizationTable4 = new HashMap<>();
        authorizationTable4.put(localAddr + ":60603", new Credentials("","login60603", "password60603"));


        new Discovery(60601, authorizationTable1).start();
        threadSleep(3000);
        new Discovery(60602, authorizationTable2).start();
        threadSleep(3000);
        new Discovery(60603, authorizationTable3).start();
        threadSleep(3000);
        new Discovery(60604, authorizationTable4).start();

    }

    private static void threadSleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
