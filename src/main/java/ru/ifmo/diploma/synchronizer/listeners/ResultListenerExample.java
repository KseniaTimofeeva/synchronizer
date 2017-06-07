package ru.ifmo.diploma.synchronizer.listeners;

import ru.ifmo.diploma.synchronizer.DirectoriesComparison;
import ru.ifmo.diploma.synchronizer.messages.AbstractMsg;

import java.util.concurrent.BlockingQueue;

/**
 * Created by ksenia on 05.06.2017.
 */
public class ResultListenerExample extends AbstractListener {

    public ResultListenerExample(String localAddr, BlockingQueue<AbstractMsg> tasks, DirectoriesComparison dc) {
        super(localAddr, tasks, dc);
    }

    @Override
    public void handle(AbstractMsg msg) {
        //@TODO
    }
}
