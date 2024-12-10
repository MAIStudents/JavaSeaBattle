package ru.mai.lessons.rpks.Server;

import ru.mai.lessons.rpks.Server.include.ConnectionHandler;
import ru.mai.lessons.rpks.Server.include.GameLogic;

import java.io.*;
import java.net.*;


public class Server {
    private static final int PORT = 8080;
    private static Socket player1 = null;
    private static Socket player2 = null;

    private volatile boolean isRunning = true;

    public static void main(String[] args) throws IOException {
        Server server = new Server();
        server.start();
    }

    public void start() throws IOException {
        while (true) {
            resetPlayers();
            run();
        }
    }

    public void run() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Сервер с портом " + PORT);
            System.out.println("Сервер запущен, ожидает подключения игроков...");

            Thread connectionMonitor = new Thread(() -> {
                while (isRunning) {
                    try {
                        if (player1 != null && player1.isClosed()) {
                            System.out.println("Игрок 1 отключился до подключения второго игрока.");
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
            ConnectionHandler connection1 = new ConnectionHandler(player1, "Игрок 1");
            connection1.startMonitoring();
            System.out.println("Игрок 1 подключен");

            player2 = serverSocket.accept();
            ConnectionHandler connection2 = new ConnectionHandler(player2, "Игрок 2");
            connection2.startMonitoring();
            System.out.println("Игрок 2 подключен");

            isRunning = false;

            GameLogic gameLogic = new GameLogic(connection1, connection2);
            gameLogic.startGameLoop();

        } catch (SocketException e) {
            System.out.println("Серверный сокет закрыт. Перезапуск сервера...");
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
}


