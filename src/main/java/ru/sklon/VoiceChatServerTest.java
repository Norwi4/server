package ru.sklon;

import javax.sound.sampled.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;

/**
 * @author Abaev Evgeniy
 */
public class VoiceChatServerTest {
    private ServerSocket nicknameServerSocket;
    private ServerSocket audioServerSocket;

    private ArrayList<ClientHandler> connectedClients; // Список подключенных клиентов

    public VoiceChatServerTest(int nicknamePort, int audioPort) throws IOException {
        nicknameServerSocket = new ServerSocket(nicknamePort);
        audioServerSocket = new ServerSocket(audioPort);

        connectedClients = new ArrayList<>(); // Инициализация списка подключенных клиентов

        System.out.println("Сервер запущен на портах " + nicknamePort + " (ник) и " + audioPort + " (аудио)");

        while (true) {
            Socket clientNicknameSocket = nicknameServerSocket.accept();
            new Thread(new ClientHandler(clientNicknameSocket)).start();
        }
    }

    private class ClientHandler implements Runnable {
        private Socket clientNicknameSocket;
        private String nickname;

        public ClientHandler(Socket socket) {
            this.clientNicknameSocket = socket;

            try (InputStream input = clientNicknameSocket.getInputStream()) {
                byte[] nicknameBuffer = new byte[32];
                input.read(nicknameBuffer);
                this.nickname = new String(nicknameBuffer).trim();
                System.out.println("Клиент с ником \"" + nickname + "\" подключен.");

                synchronized (connectedClients) { // Синхронизация доступа к списку клиентов
                    connectedClients.add(this);
                    sendUserList(); // Отправка обновленного списка пользователям
                }

                Socket clientAudioSocket = audioServerSocket.accept();
                new Thread(() -> receiveAudio(clientAudioSocket)).start();

            } catch (IOException e) {
                System.err.println("Ошибка при получении ника от клиента: " + e.getMessage());
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
                    clientNicknameSocket.close();
                    System.out.println("Клиент \"" + nickname + "\" отключен.");

                    synchronized (connectedClients) {
                        //connectedClients.remove(this); // Удаляем клиента из списка
                        sendUserList(); // Отправляем обновленный список пользователям
                    }

                } catch (IOException e) {
                    System.err.println("Ошибка при закрытии соединения с клиентом \"" + nickname + "\": " + e.getMessage());
                }
            }
        }

        private void sendUserList() throws IOException {
            StringBuilder userList = new StringBuilder();

            synchronized (connectedClients) {
                for (ClientHandler client : connectedClients) {
                    userList.append(client.nickname).append("\n"); // Добавляем никнейм клиента в список
                }
            }

            // Отправляем список всем клиентам
            for (ClientHandler client : connectedClients) {
                try {
                    OutputStream outputStream = client.clientNicknameSocket.getOutputStream();
                    outputStream.write(userList.toString().getBytes());
                    outputStream.flush();
                } catch (IOException e) {
                    System.err.println("Ошибка при отправке списка пользователям: " + e.getMessage());
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        new VoiceChatServerTest(6789, 6790); // Порты для ника и аудио
    }
}
