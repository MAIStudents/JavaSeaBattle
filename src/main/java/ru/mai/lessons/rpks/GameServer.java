package ru.mai.lessons.rpks;

import java.io.*;
import java.net.*;

public class
GameServer {
    private static final int PORT = 12345;

    private static Socket player1;
    private static Socket player2;

    private PrintWriter out1, out2;
    private BufferedReader in1, in2;
    private enum GameState {
        CHANGE_MOVE,
        STAY_MOVE,
        GAME_OVER,
        INVALID
    }

    public static void main(String[] args) {
        GameServer server = new GameServer();
        server.start();
    }


    public void start() {
       while (true) {
           runGame();
       }
    }

    public void runGame() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("New Game server started on port " + PORT);

            System.out.println("Сервер запущен, ожидает подключения игроков...");

            player1 = serverSocket.accept();
            System.out.println("Игрок 1 подключен");
            player2 = serverSocket.accept();
            System.out.println("Игрок 2 подключен");

            // Настройка потоков ввода-вывода
            out1 = new PrintWriter(player1.getOutputStream(), true);
            in1 = new BufferedReader(new InputStreamReader(player1.getInputStream()));

            out2 = new PrintWriter(player2.getOutputStream(), true);
            in2 = new BufferedReader(new InputStreamReader(player2.getInputStream()));

            boolean awating = true;
            while (awating) {
                awating = !(in1.ready() && in2.ready());
            }
            in1.readLine();
            in2.readLine();

            out1.write("START\n");
            out1.flush();
            out2.write("START\n");
            out2.flush();

            int stepFor = 0;

            out1.write("TURN\n");
            out1.flush();

            GameState state = GameState.STAY_MOVE;

            while (state != GameState.GAME_OVER) {
                if (stepFor == 0 && in1.ready()) {
                    state = makeMove(in1, in2, out1, out2);

                } else if (stepFor == 1 && in2.ready()) {  // Ход второго игрока
                    state = makeMove(in2, in1, out2, out1);
                }
                if (state == GameState.CHANGE_MOVE) {
                    stepFor = (stepFor + 1) % 2;
                } else if (state == GameState.INVALID) {
                    throw new IllegalArgumentException("how did you get this??");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeConnections();
        }
    }

    private GameState makeMove(BufferedReader current_in, BufferedReader enemy_in,
                         PrintWriter current_out, PrintWriter enemy_out ) throws IOException {
        String line = current_in.readLine();

        if (!line.equals("STEP")) {
            return GameState.INVALID;
        }

        String coords = current_in.readLine();
        enemy_out.write(line + "\n");
        enemy_out.write(coords + "\n");
        enemy_out.flush();

        String state = enemy_in.readLine();
        System.out.printf("<%s>\n", state);

        if (state.equals("LOSE")) {
            current_out.write("LOSE\n");
            current_out.write(enemy_in.readLine() + "\n");
            current_out.flush();
            return GameState.GAME_OVER;
        } else {
            String answ = enemy_in.readLine();

            current_out.write(state + "\n");
            current_out.write(answ + "\n");
            current_out.flush();

            if (GameEvent.isMissed(GameEvent.getEvents(answ))) {
                enemy_out.write("TURN\n");
                enemy_out.flush();
                return GameState.CHANGE_MOVE;
            } else {
                current_out.write("TURN\n");
                current_out.flush();
                return GameState.STAY_MOVE;
            }

        }
    }

    private void closeConnections() {
        try {
            player1.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        try {
            player2.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        out1.close();
        out2.close();
        try {
            in1.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        try {
            in2.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
}