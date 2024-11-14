package ru.mai.lessons.rpks;

import java.io.*;
import java.net.*;

public class GameServer {
    private static final int PORT = 12345;

    private static Socket player1;
    private static Socket player2;

    private PrintWriter out1, out2;
    private BufferedReader in1, in2;

    public static void main(String[] args) {
        GameServer server = new GameServer();
        server.start();
    }


    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);

            System.out.println("Сервер запущен, ожидает подключения игроков...");

            // Подключение двух клиентов
            player1 = serverSocket.accept();
            System.out.println("Игрок 1 подключен");
            player2 = serverSocket.accept();
            System.out.println("Игрок 2 подключен");

            // Настройка потоков ввода-вывода
            out1 = new PrintWriter(player1.getOutputStream(), true);
            in1 = new BufferedReader(new InputStreamReader(player1.getInputStream()));

            out2 = new PrintWriter(player2.getOutputStream(), true);
            in2 = new BufferedReader(new InputStreamReader(player2.getInputStream()));
            runGame();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void runGame() throws IOException {
        boolean gameRunning = true;
        boolean isPlayer1Turn = true;

        while (gameRunning) {
            if (isPlayer1Turn) {
                out1.println("YOUR_TURN");
                String move = in1.readLine();
                if (move.equals("END_GAME")) {
                    gameRunning = false;
                    out1.println("GAME_OVER");
                    out2.println("GAME_OVER");
                } else {
                    out2.println("OPPONENT_MOVE:" + move);
                    isPlayer1Turn = false;
                }
            } else {
                out2.println("YOUR_TURN");
                String move = in2.readLine();
                if (move.equals("END_GAME")) {
                    gameRunning = false;
                    out1.println("GAME_OVER");
                    out2.println("GAME_OVER");
                } else {
                    out1.println("OPPONENT_MOVE:" + move);
                    isPlayer1Turn = true;
                }
            }
        }
        closeConnections();
    }

    private void closeConnections() throws IOException {
        player1.close();
        player2.close();
    }


}
