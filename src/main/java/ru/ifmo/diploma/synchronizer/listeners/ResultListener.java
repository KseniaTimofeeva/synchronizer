package ru.ifmo.diploma.synchronizer.listeners;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.ifmo.diploma.synchronizer.DirectoriesComparison;
import ru.ifmo.diploma.synchronizer.messages.*;


import java.util.concurrent.BlockingQueue;

/*
 * Created by Юлия on 06.06.2017.
 */
public class ResultListener extends AbstractListener {
    private static final Logger LOG = LogManager.getLogger(ResultListener.class);

    public ResultListener(String localAddr, BlockingQueue<AbstractMsg> tasks, DirectoriesComparison dc){
        super(localAddr, tasks, dc);
    }

    @Override
    public void handle(AbstractMsg msg) {
        if(msg.getType()== MessageType.RESULT){
            LOG.debug("{}: Listener: RESULT ({}) {} from {}", localAddr,((ResultMsg) msg).getRequestMsg().getType(), ((ResultMsg) msg).getState(), msg.getSender());

            ResultMsg resMsg=(ResultMsg)msg;
            if(resMsg.getState()== MessageState.FAILED){
//                tasks.offer(resMsg.getRequestMsg());
            }
        }
    }
}
