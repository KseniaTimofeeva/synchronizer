package ru.ifmo.diploma.synchronizer;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Map;

/**
 * Created by ksenia on 24.05.2017.
 */
public class Utils {

    public static boolean exit;

    public static void closeSocket(Socket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }

}
