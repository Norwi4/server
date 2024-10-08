package ru.sklon;


import javax.sound.sampled.*;
import java.io.*;
import java.net.*;
/**
 * @author Abaev Evgeniy
 */
public class AudioReceiver implements Runnable {
    private Socket socket;

    public AudioReceiver(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            byte[] buffer = new byte[8192];
            InputStream in = socket.getInputStream();

            while (true) {
                int bytesRead = in.read(buffer);
                if (bytesRead == -1) break;
                VoiceChatServer.broadcast(buffer, socket); // Передаем текущий сокет
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
