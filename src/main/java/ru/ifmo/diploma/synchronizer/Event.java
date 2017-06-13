package ru.ifmo.diploma.synchronizer;

import java.nio.file.Path;
import java.nio.file.WatchEvent;

/*
 * Created by Юлия on 11.06.2017.
 */
class Event {

    private long eventTime;
    private WatchEvent event;
    private Path absolutePath;

    Event(long eventTime, WatchEvent event, Path absolutePath) {
        this.eventTime = eventTime;
        this.event = event;
        this.absolutePath = absolutePath;
    }

    long getEventTime() {
        return eventTime;
    }

    WatchEvent getEvent() {
        return event;
    }

    Path getAbsPath() {
        return absolutePath;
    }
}
