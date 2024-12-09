package ru.mai.lessons.rpks.Server;

import ru.mai.lessons.rpks.Server.include.ConnectionHandler;
import ru.mai.lessons.rpks.Server.include.GameLogic;

import java.io.*;
import java.net.*;


public class Server {
    private static final int PORT = 8080;
    private static Socket player1 = null;
    private static Socket player2 = null;

    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }

    public void start() {
        while (true) {
            resetPlayers();
            run();
        }
    }

    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Сервер с портом " + PORT);
            System.out.println("Сервер запущен, ожидает подключения игроков...");

            player1 = serverSocket.accept();
            System.out.println("Игрок 1 подключен");
            player2 = serverSocket.accept();
            System.out.println("Игрок 2 подключен");

            ConnectionHandler connection1 = new ConnectionHandler(player1, "Игрок 1");
            ConnectionHandler connection2 = new ConnectionHandler(player2, "Игрок 2");

            connection1.startMonitoring();
            connection2.startMonitoring();

            GameLogic gameLogic = new GameLogic(connection1, connection2);
            gameLogic.startGameLoop();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void resetPlayers() {
        player1 = null;
        player2 = null;
    }
}


