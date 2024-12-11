package ru.mai.lessons.rpks.Server;

import ru.mai.lessons.rpks.Server.include.ConnectionHandler;
import ru.mai.lessons.rpks.Server.include.GameManager;

import java.io.*;
import java.net.*;


public class Server {
    private static final int PORT = 8081;
    private static Socket player1 = null;
    private static Socket player2 = null;

    private volatile boolean isRunning = true;

    public static void main(String[] args) throws IOException {
        Server server = new Server();
        server.start();
    }

    public void run() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server with port " + PORT);
            System.out.println("The server is running, waiting for players to connect...");

            Thread connectionMonitor = new Thread(() -> {
                while (isRunning) {
                    try {
                        if (player1 != null && player1.isClosed()) {
                            System.out.println("Player 1 disconnected before the 2 player connected.");
                            isRunning = false;
                            try {
                                serverSocket.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            return;
                        }
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });

            connectionMonitor.start();

            player1 = serverSocket.accept();
            ConnectionHandler connection1 = new ConnectionHandler(player1, "Player 1");
            connection1.startMonitoring();
            System.out.println("Player 1 connected");

            player2 = serverSocket.accept();
            ConnectionHandler connection2 = new ConnectionHandler(player2, "Player 2");
            connection2.startMonitoring();
            System.out.println("Player 2 connected");

            isRunning = false;

            GameManager gameManager = new GameManager(connection1, connection2);
            gameManager.startGameLoop();

        } catch (SocketException e) {
            System.out.println("Server socket closed. Restarting server...");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            resetPlayers();
        }
    }


    private void resetPlayers() throws IOException {
        if (player1 != null && !player1.isClosed()) {
            player1.close();
        }
        if (player2 != null && !player2.isClosed()) {
            player2.close();
        }
        player1 = null;
        player2 = null;
    }

    public void start() throws IOException {
        while (true) {
            resetPlayers();
            run();
        }
    }
}


