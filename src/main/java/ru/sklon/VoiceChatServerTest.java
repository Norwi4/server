package ru.sklon;

import javax.sound.sampled.*;
import java.io.*;
import java.net.*;
/**
 * @author Abaev Evgeniy
 */
public class VoiceChatServerTest {
    private ServerSocket serverSocket;

    public VoiceChatServerTest(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("Сервер запущен на порту " + port);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("Клиент подключен: " + clientSocket.getInetAddress());
            new Thread(new ClientHandler(clientSocket)).start();
        }
    }

    private class ClientHandler implements Runnable {
        private Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        public void run() {
            try (InputStream input = clientSocket.getInputStream()) {
                AudioFormat format = new AudioFormat(44100, 16, 2, true, true);
                SourceDataLine line = AudioSystem.getSourceDataLine(format);
                line.open(format);
                line.start();

                byte[] buffer = new byte[4096];
                int bytesRead;

                while ((bytesRead = input.read(buffer)) != -1) {
                    line.write(buffer, 0, bytesRead); // Воспроизведение звука
                }
            } catch (IOException | LineUnavailableException e) {
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                    System.out.println("Клиент отключен: " + clientSocket.getInetAddress());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        new VoiceChatServerTest(6789);
    }
}
