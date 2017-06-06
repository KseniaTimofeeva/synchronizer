package ru.ifmo.diploma.synchronizer.listeners;

import ru.ifmo.diploma.synchronizer.DirectoriesComparison;
import ru.ifmo.diploma.synchronizer.messages.AbstractMsg;

import java.util.concurrent.BlockingQueue;

/*
 * Created by Юлия on 04.06.2017.
 */
public abstract class AbstractListener implements Listener<AbstractMsg>{
    String localAddr;
    BlockingQueue<AbstractMsg> tasks;
    DirectoriesComparison dc;

    public AbstractListener(String localAddr, BlockingQueue<AbstractMsg> tasks, DirectoriesComparison dc){
       this.localAddr=localAddr;
       this.tasks=tasks;
       this.dc=dc;
    }

}
