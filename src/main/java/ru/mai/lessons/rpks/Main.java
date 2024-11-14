package ru.mai.lessons.rpks;

import ru.mai.lessons.rpks.Message;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class Main {
    private static final int PORT = 12345;
    private static List<ClientHandler> clients = new ArrayList<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected");

                ClientHandler clientHandler = new ClientHandler(socket);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void broadcastMessage(Message message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket socket;
        private ObjectOutputStream out;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
                out = new ObjectOutputStream(socket.getOutputStream());

                Message message;
                while ((message = (Message) in.readObject()) != null) {
                    System.out.println("Received: " + message);
                    broadcastMessage(message);
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        public void sendMessage(Message message) {
            try {
                out.writeObject(message);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
