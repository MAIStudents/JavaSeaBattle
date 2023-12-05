package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    public static final int PORT = 8080;

    public static ClientManager clientOne = null;

    public static ClientManager clientTwo = null;

    public static ServerSocket serverSocket = null;

    public static void main(String[] args) {

        try {
            serverSocket = new ServerSocket(PORT);

            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                if (clientOne == null) {
                    clientOne = new ClientManager(socket);
                    new Thread(clientOne).start();
                } else if (clientTwo == null) {
                    clientTwo = new ClientManager(socket);
                    new Thread(clientTwo).start();
                    clientOne.sendMessage("ORDER 1");
                    clientTwo.sendMessage("ORDER 2");
                }
            }
        } catch (IOException ignored) {
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException ignored) {
                    System.err.println("Failed to close server socket!");
                }
            }
        }
    }

    public static ClientManager getOpponent(ClientManager current) {
        if (current.equals(clientOne)) {
            return clientTwo;
        } else {
            return clientOne;
        }
    }

}