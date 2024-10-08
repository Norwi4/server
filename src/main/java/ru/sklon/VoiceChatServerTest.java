package ru.sklon;

import javax.sound.sampled.*;
import java.io.*;
import java.net.*;
/**
 * @author Abaev Evgeniy
 */
public class VoiceChatServerTest {
    private ServerSocket nicknameServerSocket;
    private ServerSocket audioServerSocket;

    public VoiceChatServerTest(int nicknamePort, int audioPort) throws IOException {
        nicknameServerSocket = new ServerSocket(nicknamePort);
        audioServerSocket = new ServerSocket(audioPort);

        System.out.println("Сервер запущен на портах " + nicknamePort + " (ник) и " + audioPort + " (аудио)");

        while (true) {
            Socket clientNicknameSocket = nicknameServerSocket.accept();
            new Thread(new ClientHandler(clientNicknameSocket)).start(); // Создание нового потока для каждого клиента
        }
    }

    private class ClientHandler implements Runnable {
        private Socket clientNicknameSocket;
        private String nickname;

        public ClientHandler(Socket socket) {
            this.clientNicknameSocket = socket;

            // Получение ника от клиента
            try (InputStream input = clientNicknameSocket.getInputStream()) {
                byte[] nicknameBuffer = new byte[32];
                input.read(nicknameBuffer);
                this.nickname = new String(nicknameBuffer).trim();
                System.out.println("Клиент с ником \"" + nickname + "\" подключен.");

                // Ожидание подключения для аудио
                Socket clientAudioSocket = audioServerSocket.accept();
                new Thread(() -> receiveAudio(clientAudioSocket)).start();

            } catch (IOException e) {
                System.err.println("Ошибка при получении ника от клиента: " + e.getMessage());
                return;
            }
        }

        @Override
        public void run() { }

        public void receiveAudio(Socket clientAudioSocket) {
            try (InputStream input = clientAudioSocket.getInputStream()) {
                AudioFormat format = new AudioFormat(44100, 16, 1, true, true);
                SourceDataLine line = AudioSystem.getSourceDataLine(format);
                line.open(format);
                line.start();

                byte[] buffer = new byte[4096];
                int bytesRead;

                while ((bytesRead = input.read(buffer)) != -1) {
                    line.write(buffer, 0, bytesRead);
                }

            } catch (IOException | LineUnavailableException e) {
                System.err.println("Ошибка при получении аудио от клиента \"" + nickname + "\": " + e.getMessage());

            } finally {
                try {
                    clientAudioSocket.close();
                    clientNicknameSocket.close();
                    System.out.println("Клиент \"" + nickname + "\" отключен.");
                } catch (IOException e) {
                    System.err.println("Ошибка при закрытии соединения с клиентом \"" + nickname + "\": " + e.getMessage());
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        new VoiceChatServerTest(6789, 6790); // Порты для ника и аудио
    }
}
