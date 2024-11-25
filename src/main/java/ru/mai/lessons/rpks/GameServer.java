package ru.mai.lessons.rpks;

import java.io.*;
import java.net.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static ru.mai.lessons.rpks.BattleMessage.*;

public class GameServer {
    private static final int PORT = 12347;
    private static Socket player1 = null;
    private static Socket player2 = null;

    private PrintWriter out1, out2;
    private BufferedReader in1, in2;

    private Long diffBeat1 = 0L;
    private Long diffBeat2 = 0L;

    private final Long MAX_BEAT_WAIT = 7000L;

    private final BlockingQueue<BattleMessage> player1Queue = new LinkedBlockingQueue<>();
    private final BlockingQueue<BattleMessage> player2Queue = new LinkedBlockingQueue<>();

    private enum GameState {
        WAITING,
        CHANGE_MOVE,
        NOPE,
        STAY_MOVE,
        GAME_OVER,
        INVALID,
        HEARTBEAT,
    }

    public static void main(String[] args) {
        GameServer server = new GameServer();
        server.start();
    }

    public void start() {
        while (true) {
            player1 = null;
            player2 = null;
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

            out1 = new PrintWriter(player1.getOutputStream(), true);
            in1 = new BufferedReader(new InputStreamReader(player1.getInputStream()));

            out2 = new PrintWriter(player2.getOutputStream(), true);
            in2 = new BufferedReader(new InputStreamReader(player2.getInputStream()));

            // Запускаем мониторинг потоков
            startInputMonitoring(player1, in1, out1, player1Queue, "Игрок 1");
            startInputMonitoring(player2, in2, out2, player2Queue, "Игрок 2");

            // Отправляем стартовые сообщения
            out1.write("6\n");
            out2.write("6\n");
            out1.flush();
            out2.flush();

            out1.write("3\n");
            out1.flush();

            int stepFor = 0;
            GameState state = GameState.NOPE;

            diffBeat1 = System.currentTimeMillis();
            diffBeat2 = System.currentTimeMillis();

            while (state != GameState.GAME_OVER) {
                BattleMessage message1 = getNextMessage(player1Queue);
                BattleMessage message2 = getNextMessage(player2Queue);

                if (message1 != null && message1.messageType == MessageType.HEARTBEAT) {
                    diffBeat1 = System.currentTimeMillis();
                    message1 = null;
                }
                if (message2 != null && message2.messageType == MessageType.HEARTBEAT) {
                    diffBeat2 = System.currentTimeMillis();
                    message2 = null;
                }

                var curr = System.currentTimeMillis();
                if (curr - diffBeat1 > MAX_BEAT_WAIT || curr - diffBeat2 > MAX_BEAT_WAIT) {
                    disconnected(out1);
                    disconnected(out2);
                    throw new RuntimeException("someone disconnected");
                }

                if (stepFor == 0 && state == GameState.NOPE && message1 != null) {
                    BattleMessage msg = message1;
                    state = makeMove(msg, out1, out2);
                } else if (stepFor == 1 && state == GameState.NOPE && message2 != null) {
                    BattleMessage msg = message2;
                    state = makeMove(msg, out2, out1);
                } else if (stepFor == 0 && state == GameState.WAITING && message2 != null) {
                    BattleMessage msg = message2;
                    state = makeMove(msg, out1, out2);
                } else if (stepFor == 1 && state == GameState.WAITING && message1 != null) {
                    BattleMessage msg = message1;
                    state = makeMove(msg, out2, out1);
                }

                if (state == GameState.CHANGE_MOVE) {
                    stepFor = (stepFor + 1) % 2;
                    state = GameState.STAY_MOVE;
                }
                if (state == GameState.STAY_MOVE) {
                    if (stepFor == 0) {
                        out1.write("3\n");
                        out1.flush();
                    } else {
                        out2.write("3\n");
                        out2.flush();
                    }
                    state = GameState.NOPE;
                }
            }
        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
        } finally {
            closeConnections();
        }
    }

    private void startInputMonitoring(Socket player, BufferedReader in, PrintWriter out, BlockingQueue<BattleMessage> queue, String playerName) {
        new Thread(() -> {
            try {
                int counter = 0;
                while (!player.isClosed()) {
                    if (in.ready()) {
                        BattleMessage message = getMessageFromString(in.readLine());
                        queue.put(message);
                    }
                    Thread.sleep(100);
                    if (counter % 50 == 0) {
                        out.write("0\n");
                        out.flush();
                    }
                    counter++;
                }
            } catch (Exception e) {
                System.out.println(playerName + " отключился: " + e.getMessage());
                closeConnections();
            }
        }).start();
    }

    private BattleMessage getNextMessage(BlockingQueue<BattleMessage> queue) {
        return queue.poll();
    }

    private GameState makeMove(BattleMessage msg, PrintWriter out1, PrintWriter out2) throws IOException {
        if (msg.messageType == MessageType.STEP) {
            out2.write(msg.toString() + "\n");
            out2.flush();
            return GameState.WAITING;
        } else if (msg.messageType == MessageType.GAME_OVER) {
            System.out.printf("%s\n", msg.toString());
            out1.write(msg.toString() + "\n");
            out1.flush();
            return GameState.GAME_OVER;
        } else if (msg.messageType == MessageType.RESPONSE) {
            out1.write(msg.toString() + "\n");
            out1.flush();
            if (GameEvent.isMissed(msg.gameEvents)) {
                return GameState.CHANGE_MOVE;
            }
            return GameState.STAY_MOVE;
        } else if (msg.messageType == MessageType.HEARTBEAT) {
            return GameState.HEARTBEAT;
        }
        return GameState.INVALID;
    }
    private void disconnected(PrintWriter out) {
        out.write("4\n");
        out.flush();
    }

    private void closeConnections() {
        try {
            if (player1 != null) player1.close();
            if (player2 != null) player2.close();
            if (out1 != null) out1.close();
            if (out2 != null) out2.close();
            if (in1 != null) in1.close();
            if (in2 != null) in2.close();
        } catch (IOException e) {
            System.out.println("Ошибка при закрытии соединений: " + e.getMessage());
        }
    }
}
