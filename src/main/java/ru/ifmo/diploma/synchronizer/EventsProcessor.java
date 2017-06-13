package ru.ifmo.diploma.synchronizer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.ifmo.diploma.synchronizer.messages.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.BlockingQueue;

/*
 * Created by Юлия on 12.06.2017.
 */
public class EventsProcessor implements Runnable {
    private static final Logger LOG = LogManager.getLogger(EventsProcessor.class);

    private String localAddr;
    private BlockingQueue<Event> events;
    private final String startPath;
    private BlockingQueue<FileOperation> fileOperations;
    private BlockingQueue<AbstractMsg> tasks;

    public EventsProcessor(String localAddr, BlockingQueue<Event> events, String path, BlockingQueue<FileOperation> fileOperations, BlockingQueue<AbstractMsg> tasks) {
        this.localAddr = localAddr;
        this.events = events;
        startPath = path;
        this.fileOperations = fileOperations;
        this.tasks = tasks;
    }

    private String getRelativePath(Path p) {
        return p.toString().substring(startPath.length() + 1);
    }

    private AbstractMsg sendFileBroadcast(Path filePath){
        AbstractMsg msg=null;
        File f = new File(filePath.toString());
        try (InputStream in = new FileInputStream(f);
             ByteArrayOutputStream bout = new ByteArrayOutputStream()) {

            int l;
            byte[] buf = new byte[1024];
            while ((l = in.read(buf)) > 0) {
                bout.write(buf, 0, l);
            }
            BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);
            msg=new FileMsg(localAddr, bout.toByteArray(), getRelativePath(filePath), attrs.creationTime().toMillis());


        } catch (IOException e) {
            e.printStackTrace();
        }
        return msg;
    }

    @Override
    public void run() {
        while (true) {
            Event event;
            FileOperation operation = null;
            AbstractMsg msg = null;
            if ((event = events.poll()) != null) {

                String kind = event.getEvent().kind().name();
                Path prevAbsPath = null;
                try {
                    Thread.sleep(20);

                    Event createEvent, modEvent;

                    switch (kind) {
                        case "ENTRY_CREATE":
                            if ((modEvent = events.peek()) != null) {
                                if ("ENTRY_MODIFY".equals(modEvent.getEvent().kind().name()) &&
                                        (modEvent.getEventTime() - event.getEventTime()) < 300) {
                                    prevAbsPath = modEvent.getAbsPath();
                                    events.poll();
                                    Thread.sleep(20);
                                    if ((modEvent = events.peek()) != null) {
                                        if (!("ENTRY_MODIFY".equals(modEvent.getEvent().kind().name()) &&
                                                (modEvent.getEventTime() - event.getEventTime()) < 300)) {
                                            prevAbsPath = modEvent.getAbsPath();
                                            events.poll();
                                            Thread.sleep(20);
                                        }

                                    }
                                }
                                operation = new FileOperation(OperationType.ENTRY_COPY_OR_CREATE, event.getEvent().context().toString());
                                msg=sendFileBroadcast(event.getAbsPath());
                                LOG.debug("%s: %s %s\n", "ENTRY_COPY", event.getEvent().context(), prevAbsPath);

                                break;
                            }
                            operation = new FileOperation(OperationType.ENTRY_CREATE, event.getEvent().context().toString());
                            msg=sendFileBroadcast(event.getAbsPath());
                            LOG.debug("%s %s: %s %s\n", event.getEventTime(), kind, event.getEvent().context());

                            //send new file to all hosts
                            break;
                        case "ENTRY_MODIFY":
                            if ((modEvent = events.peek()) != null) {
                                if ("ENTRY_MODIFY".equals(modEvent.getEvent().kind().name()) &&
                                        (modEvent.getEventTime() - event.getEventTime()) < 300) {

                                    events.poll();
                                    Thread.sleep(20);

                                }
                            }
                            operation = new FileOperation(OperationType.ENTRY_MODIFY, event.getEvent().context().toString());
                            msg=sendFileBroadcast(event.getAbsPath());

                            LOG.debug("%s %s: %s %s\n", event.getEventTime(), kind, event.getEvent().context());

                            break;
                        case "ENTRY_DELETE":

                            if ((createEvent = events.peek()) != null) {
                                if ("ENTRY_CREATE".equals(createEvent.getEvent().kind().name()) &&
                                        (createEvent.getEventTime() - event.getEventTime()) < 300) {
                                    prevAbsPath = createEvent.getAbsPath();

                                    events.poll();
                                    Thread.sleep(20);
                                    if ((modEvent = events.peek()) != null) {
                                        if ("ENTRY_MODIFY".equals(modEvent.getEvent().kind().name()) &&
                                                (modEvent.getEventTime() - createEvent.getEventTime()) < 300) {
                                            prevAbsPath = modEvent.getAbsPath();

                                            events.poll();
                                            Thread.sleep(20);
                                        }

                                    }
                                    if (createEvent.getEvent().context().equals(event.getEvent().context())) {
                                        operation = new FileOperation(OperationType.ENTRY_MOVE, event.getEvent().context().toString());
                                        msg = new TransferFileMsg(localAddr, getRelativePath(event.getAbsPath()), getRelativePath(prevAbsPath));

                                        LOG.debug("%s: %s %s\n", "ENTRY_MOVE", event.getEvent().context(), prevAbsPath);
                                    } else {
                                        operation = new FileOperation(OperationType.ENTRY_RENAME, event.getEvent().context().toString());
                                        msg = new RenameFileMsg(localAddr, getRelativePath(event.getAbsPath()), getRelativePath(prevAbsPath));
                                        LOG.debug("%s: %s %s\n", "ENTRY_RENAME", getRelativePath(event.getAbsPath()), prevAbsPath);
                                    }

                                    break;
                                }
                            }
                            operation = new FileOperation(OperationType.ENTRY_DELETE, event.getEvent().context().toString());
                            msg = new DeleteFileMsg(localAddr, getRelativePath(event.getAbsPath()));

                            LOG.debug("%s %s: %s %s\n", event.getEventTime(), kind, event.getEvent().context());

                            //notify all hosts to delete this file
                            break;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                FileOperation curOperation;
                if ((curOperation = fileOperations.peek()) != null) {

                    if (operation != null) {
                        if (!curOperation.equals(operation)) {
                            LOG.debug("{} sent {}", localAddr, msg.getType());
                            tasks.offer(msg);
                        } else {
                            LOG.debug("{} ignored {}", localAddr, curOperation.type);
                            fileOperations.poll();
                        }
                    }
                } else
                    tasks.offer(msg);

            }

        }
    }
}
