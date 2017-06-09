package ru.ifmo.diploma.synchronizer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.nio.file.StandardWatchEventKinds.*;


/*
 * Created by Юлия on 18.05.2017.
 */
public class DirectoryChangesViewer implements Runnable {
    private static final Logger LOG = LogManager.getLogger(DirectoryChangesViewer.class);

    private final WatchService watcher;
    private final Map<WatchKey, Path> keys;
    private String localAddr;


    DirectoryChangesViewer(Path dir, String localAddr) throws IOException {
        this.watcher = FileSystems.getDefault().newWatchService();
        this.keys = new HashMap<WatchKey, Path>();
        this.localAddr = localAddr;

        walkAndRegisterDirectories(dir);
    }


    private void walkAndRegisterDirectories(final Path start) throws IOException {

        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
                keys.put(key, dir);

                return FileVisitResult.CONTINUE;
            }
        });
    }


    @Override
    public void run() {
        LOG.debug("Directory changes viewer started on {}", localAddr);

        while (true) {

            WatchKey key;
            try {
                key = watcher.take();
            } catch (InterruptedException x) {
                return;
            }

            Path dir = keys.get(key);

            for (WatchEvent event : key.pollEvents()) {
                String kind = event.kind().name();

                Path watchedObj = dir.resolve(((WatchEvent<Path>) event).context());

                try {
                    if (Files.isDirectory(watchedObj)) {
                        if (kind.equals("ENTRY_CREATE"))
                            walkAndRegisterDirectories(watchedObj);
                    } else {
                        switch (kind) {
                            case "ENTRY_CREATE":
                                System.out.println("ENTRY_CREATE");
                                //send new file to all hosts
                                break;
                            case "ENTRY_MODIFY":
                                System.out.println("ENTRY_MODIFY");
                                //send delta to all hosts
                                break;
                            case "ENTRY_DELETE":
                                System.out.println("ENTRY_DELETE");
                                //notify all hosts to delete this file
                                break;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

            if (!key.reset()) {
                keys.remove(key);

                if (keys.isEmpty()) {
                    break;
                }
            }
        }
    }

}
