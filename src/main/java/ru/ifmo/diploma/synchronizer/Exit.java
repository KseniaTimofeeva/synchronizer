package ru.ifmo.diploma.synchronizer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.ifmo.diploma.synchronizer.discovery.CurrentConnections;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Created by ksenia on 08.06.2017.
 */
public class Exit extends Thread {
    private static final Logger LOG = LogManager.getLogger(Exit.class);

    private Synchronizer synchronizer;
    private List<Thread> threadList;
    private Map<String, CurrentConnections> connections;
    private String startPath;
    private DirectoriesComparison dc;
    private String localAddr;

    public Exit(Synchronizer synchronizer) {
        this.synchronizer = synchronizer;
        this.threadList = synchronizer.getThreadList();
        this.connections = synchronizer.getConnections();
        this.startPath = synchronizer.getStartPath();
        this.dc = synchronizer.getDc();
        this.localAddr = synchronizer.getLocalAddr();
    }

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);
        while (true) {

            String string = scanner.nextLine();

            if (string.equals("q")) {
                LOG.trace("Start ending host " + localAddr);

                dc.saveDirectoryState(dc.getAbsolutePath("log.bin"));

                if (synchronizer.getServerSocket() != null) {
                    try {
                        synchronizer.getServerSocket().close();
                    } catch (IOException e) {
                        LOG.debug("{}: Server socket ending error", localAddr);
                    }
                }

                if (!connections.isEmpty()) {
                    for (Map.Entry<String, CurrentConnections> entry : connections.entrySet()) {
                        LOG.trace("{}: End: interrupt {} reader", localAddr, entry.getKey());

                        entry.getValue().getThread().interrupt();
                    }
                }

                if (!threadList.isEmpty()) {
                    for (Thread t : threadList) {
                        LOG.trace("{}: End: interrupt thread", localAddr);

                        t.interrupt();
                    }
                }

                LOG.debug("END HOST " + localAddr);
                break;
            }
        }
    }
}
