package ru.mai.lessons.rpks;

import ru.mai.lessons.rpks.client.GameController;
import ru.mai.lessons.rpks.logger.Logger;
import ru.mai.lessons.rpks.utils.Message;
import ru.mai.lessons.rpks.utils.Point;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Server {

    private static final int PORT = 18088;
    private Socket firstPlayer;
    private Socket secondPlayer;

    private PrintWriter out1;
    private PrintWriter out2;
    private BufferedReader in1;
    private BufferedReader in2;

    private Logger logger = new Logger(getClass());

    private final Long MAX_BEAT_WAIT = 7000L;

    private boolean disconnected = false;

    public static void main(String[] args) {
        Server server = new Server();

        server.start();
    }

    private void start() {
        while (true) {
            firstPlayer = null;
            secondPlayer = null;
            disconnected = false;
            logger.info("Starting new game");
            runGame();
        }
    }

    private static enum GameState {
        accepting, first_turn, second_turn, end
    }

    private void runGame() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            logger.info("New game started on port {}", PORT);

            firstPlayer = serverSocket.accept();

            logger.info("First player connected");

            secondPlayer = serverSocket.accept();

            logger.info("Second player connected");

            PrintWriter out1 = new PrintWriter(firstPlayer.getOutputStream(), true);
            PrintWriter out2 = new PrintWriter(secondPlayer.getOutputStream(), true);
            BufferedReader in1 = new BufferedReader(new InputStreamReader(firstPlayer.getInputStream()));
            BufferedReader in2 = new BufferedReader(new InputStreamReader(secondPlayer.getInputStream()));
            BlockingQueue<Message> player1Queue = new LinkedBlockingQueue<>();
            BlockingQueue<Message> player2Queue = new LinkedBlockingQueue<>();

            startInputMonitoring(firstPlayer, in1, out1, player1Queue, "First player");
            startInputMonitoring(secondPlayer, in2, out2, player2Queue, "Second player");

            boolean firstAccepted = false;
            boolean secondAccepted = false;

            GameState state = GameState.accepting;

            final List<List<Point>> firstField = new ArrayList<>(10);
            final List<List<Point>> secondField = new ArrayList<>(10);

            while (state != GameState.end && !disconnected) {
                if (state == GameState.accepting) {

                    Message firstMessage = player1Queue.poll();

                    if (firstMessage != null) {
                        firstAccepted = checkAndFillField(firstField, firstMessage.getContent());

                        Message message = new Message(Message.MessageType.accept,  firstAccepted ? "1" : "0");

                        out1.write(message.toString());
                        out1.flush();
                    }

                    Message secondMessage = player2Queue.poll();

                    if (secondMessage != null && checkAndFillField(secondField, secondMessage.getContent())) {
                        secondAccepted = checkAndFillField(secondField, secondMessage.getContent());

                        Message message = new Message(Message.MessageType.accept,  secondAccepted ? "1" : "0");

                        out2.write(message.toString());
                        out2.flush();
                    }

                    if (firstAccepted && secondAccepted) {
                        state = GameState.first_turn;

                        Message message = new Message(Message.MessageType.start,  "1");

                        out1.write(message.toString());
                        out1.flush();
                        out2.write(message.toString());
                        out2.flush();

                        Message attackMessage = new Message(Message.MessageType.attack, "1");
                        attackMessage.setIsForMe(true);

                        out1.write(attackMessage.toString());
                        out1.flush();

                        attackMessage.setIsForMe(false);

                        out2.write(attackMessage.toString());
                        out2.flush();
                    }

                    continue;
                }
            }


        } catch (IOException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        } finally {
            closeConnections();
        }
    }

    private static class Index {
        public int x;
        public int y;

        public Index(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Index index = (Index) o;
            return x == index.x && y == index.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }

    private boolean checkAndFillField(List<List<Point>> field, String data) {
        field.clear();
        if (data.length() != 100) {
            logger.error("Wrong field length");
        }

        Map<Index, Boolean> mappedField = new HashMap<>();

        for (int i = 0; i < 10; i++) {
            List<Point> lst = new ArrayList<>(10);
            for (int j = 0; j < 10; j++) {
                boolean res = data.charAt(i * 10 + j) == '1';
                lst.add(res ? new Point(true) : new Point(false));
                mappedField.put(new Index(i, j), res);
            }
            field.add(lst);
        }

        int ship4 = 0, ship3 = 0, ship2 = 0, ship1 = 0;

        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                Index ind = new Index(i, j);
                if (!mappedField.containsKey(ind) || !mappedField.get(ind)) {
                    continue;
                }

                int res = checkShip(mappedField, i, j);

                switch (res) {
                    case 0 -> {
                        return false;
                    }
                    case 1 -> {
                        ++ship1;
                        break;
                    }
                    case 2 -> {
                        ++ship2;
                        break;
                    }
                    case 3 -> {
                        ++ship3;
                        break;
                    }
                    case 4 -> {
                        ++ship4;
                        break;
                    }
                }
            }
        }

        return ship1 == 4 && ship2 == 3 && ship3 == 2 && ship4 == 1;
    }

    private int checkShip(Map<Index, Boolean> mappedField, int x, int y) {
        int res = 1;

        int minx = x, maxx = x;
        int miny = y, maxy = y;
        boolean needSearch = true;

        Index ind = new Index(x, y);

        while (minx > 0 && needSearch) {
            --minx;
            ind.x = minx;
            if (!mappedField.containsKey(ind)) {
                return 0;
            }
            if (!mappedField.get(ind)) {
                ++minx;
                needSearch = false;
            }
        }

        needSearch = true;

        while (maxx < 9 && needSearch) {
            ++maxx;
            ind.x = maxx;
            if (!mappedField.containsKey(ind)) {
                return 0;
            }
            if (!mappedField.get(ind)) {
                --maxx;
                needSearch = false;
            }
        }

        needSearch = true;
        ind.x = x;

        while (miny > 0 && needSearch) {
            --miny;
            ind.y = miny;
            if (!mappedField.containsKey(ind)) {
                return 0;
            }
            if (!mappedField.get(ind)) {
                ++miny;
                needSearch = false;
            }
        }

        needSearch = true;

        while (maxy < 9 && needSearch) {
            ++maxy;
            ind.y = maxy;
            if (!mappedField.containsKey(ind)) {
                return 0;
            }
            if (!mappedField.get(ind)) {
                --maxy;
                needSearch = false;
            }
        }

        if(maxy - miny != 0 && maxx - minx != 0) {
            return 0;
        }

        res = Math.max(maxx - minx, maxy - miny) + 1;

        for (int i = minx - 1; i <= maxx + 1; i++) {
            if (i < 0 || i > 9) {
                continue;
            }
            for (int j = miny - 1; j <= maxy + 1; j++) {
                if(j < 0 || j > 9) {
                    continue;
                }

                ind.x = i;
                ind.y = j;

                if (i != minx - 1 && i != maxx + 1 && j != miny - 1 && j != maxy + 1) {
                    mappedField.remove(ind);
                } else if (!mappedField.containsKey(ind) || mappedField.get(ind)) {
                    return 0;
                }
            }
        }

        return res;
    }

    private void startInputMonitoring(Socket player, BufferedReader in, PrintWriter out, BlockingQueue<Message> queue, String playerName) {
        new Thread(() -> {
            try {
                Long diffBeat = 0L;

                Long previousTime = System.currentTimeMillis();

                boolean wasHeart = false;

                while (!player.isClosed()) {
                    if (in.ready()) {
                        Message message = Message.fromString(in.readLine());

                        if (message.getType() == Message.MessageType.heart) {
                            wasHeart = false;
                            logger.info("Received heart message from {}", playerName);
                        } else {
                            queue.put(message);
                        }
                    }
                    Thread.yield();

                    Long newTime = System.currentTimeMillis();

                    diffBeat += newTime - previousTime;
                    previousTime = newTime;

                    if (diffBeat > MAX_BEAT_WAIT) {
                        if (wasHeart) {
                            throw new RuntimeException(playerName + " disconnected");
                        } else {
                            out.write((new Message(Message.MessageType.heart, "1").toString()));
                            out.flush();
                            logger.info("Sent heart message to {}", playerName);
                            wasHeart = true;
                            diffBeat = 0L;
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("{} broke connection: {}", playerName, e.getMessage());
                disconnected = true;
                closeConnections();
            }
        }).start();
    }

    private void closeConnections() {
        try {
            if (firstPlayer != null) firstPlayer.close();
            if (secondPlayer != null) secondPlayer.close();
            if (out1 != null) out1.close();
            if (out2 != null) out2.close();
            if (in1 != null) in1.close();
            if (in2 != null) in2.close();
        } catch (IOException e) {
            System.out.println("Ошибка при закрытии соединений: " + e.getMessage());
        }
    }
}
