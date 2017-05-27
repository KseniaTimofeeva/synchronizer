package ru.ifmo.diploma.synchronizer.discovery;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Map;

/**
 * Created by ksenia on 24.05.2017.
 */
class Utils {

    static void closeSocket(Socket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }

}
