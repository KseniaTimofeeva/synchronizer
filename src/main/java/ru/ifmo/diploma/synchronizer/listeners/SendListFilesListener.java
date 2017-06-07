package ru.ifmo.diploma.synchronizer.listeners;

import ru.ifmo.diploma.synchronizer.DirectoriesComparison;
import ru.ifmo.diploma.synchronizer.messages.*;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.concurrent.BlockingQueue;

/*
 * Created by Юлия on 04.06.2017.
 */
public class SendListFilesListener extends AbstractListener {
    ObjectOutputStream out;

    public SendListFilesListener(String localAddr, BlockingQueue<AbstractMsg> tasks, DirectoriesComparison dc, ObjectOutputStream out) {
        super(localAddr, tasks, dc);
        this.out = out;
    }

    @Override
    public void handle(AbstractMsg msg) {

        if (msg.getType() == MessageType.SEND_LIST_FILES) {

            //??? Отправляет ResultMsg, что список файлов получен
            //??? запускает сравнение списков файлов
            //??? dc кладет сообщения в writer


            try {
                out.writeObject(dc.getListFiles());
                out.flush();
                tasks.offer(new ResultMsg(msg.getSender(), MessageState.SUCCESS, msg.getType())); //?ответ должен слать тот, кто получил список

            } catch (IOException e) {
                tasks.offer(new ResultMsg(msg.getSender(), MessageState.FAILED, msg.getType()));

            }
        }
    }
}
