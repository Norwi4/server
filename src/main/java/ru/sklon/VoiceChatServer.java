package ru.sklon;

import javax.sound.sampled.*;
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * @author Abaev Evgeniy
 */
public class VoiceChatServer {
    private static final int PORT = 50005;
    private static Set<Socket> clientSockets = new HashSet<>();

    public static void main(String[] args) {
        System.out.println("Server started...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                clientSockets.add(clientSocket);
                new Thread(new AudioReceiver(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Измененный метод broadcast с исключением отправителя
    static void broadcast(byte[] audioData, Socket senderSocket) {
        for (Socket socket : clientSockets) {
            if (socket != senderSocket) { // Проверяем, что это не тот же сокет
                try {
                    OutputStream out = socket.getOutputStream();
                    out.write(audioData);
                    out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
