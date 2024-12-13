package ru.mai.lessons.rpks;

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
import java.util.concurrent.atomic.AtomicBoolean;

public class Server {

    private static final int PORT = 18088;
    private Socket firstPlayer;
    private Socket secondPlayer;

    private PrintWriter out1;
    private PrintWriter out2;
    private BufferedReader in1;
    private BufferedReader in2;

    private final Logger logger = new Logger(getClass());

    private final Long MAX_BEAT_WAIT = 7000L;

    private final AtomicBoolean disconnected = new AtomicBoolean(false);

    public static void main(String[] args) {
        Server server = new Server();

        server.start();
    }

    private void start() {
        while (true) {
            firstPlayer = null;
            secondPlayer = null;
            disconnected.set(false);
            logger.info("Starting new game");
            runGame();
        }
    }

    private static enum GameState {
        accepting, firstTurn, secondTurn, end
    }

    private void runGame() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            logger.info("New game started on port {}", PORT);

            firstPlayer = serverSocket.accept();

            logger.info("First player connected");

            secondPlayer = serverSocket.accept();

            logger.info("Second player connected");

            out1 = new PrintWriter(firstPlayer.getOutputStream(), true);
            out2 = new PrintWriter(secondPlayer.getOutputStream(), true);
            in1 = new BufferedReader(new InputStreamReader(firstPlayer.getInputStream()));
            in2 = new BufferedReader(new InputStreamReader(secondPlayer.getInputStream()));
            BlockingQueue<Message> player1Queue = new LinkedBlockingQueue<>();
            BlockingQueue<Message> player2Queue = new LinkedBlockingQueue<>();

            startInputMonitoring(firstPlayer, in1, out1, player1Queue, "First player");
            startInputMonitoring(secondPlayer, in2, out2, player2Queue, "Second player");

            boolean firstAccepted = false;
            boolean secondAccepted = false;

            GameState state = GameState.accepting;

            final List<List<Point>> firstField = new ArrayList<>(10);
            final List<List<Point>> secondField = new ArrayList<>(10);

            while (state != GameState.end && !disconnected.get()) {
                if (state == GameState.accepting) {

                    Message firstMessage = player1Queue.poll();

                    if (firstMessage != null) {
                        firstAccepted = fillField(firstField, firstMessage.getContent());
                        if (firstAccepted) {
                            firstAccepted = checkField(firstField);
                        }
//                        firstAccepted = fillField(firstField, firstMessage.getContent());

                        Message message = new Message(Message.MessageType.accept,  firstAccepted ? "1" : "0");

                        out1.write(message.toString());
                        out1.flush();
                    }

                    Message secondMessage = player2Queue.poll();

                    if (secondMessage != null) {
                        secondAccepted = fillField(secondField, secondMessage.getContent());
                        if (secondAccepted) {
                            secondAccepted = checkField(secondField);
                        }
//                        secondAccepted = fillField(secondField, secondMessage.getContent());

                        Message message = new Message(Message.MessageType.accept,  secondAccepted ? "1" : "0");

                        out2.write(message.toString());
                        out2.flush();
                    }

                    if (firstAccepted && secondAccepted) {
                        state = GameState.firstTurn;

                        logger.info("Accepted players");

                        Message message = new Message(Message.MessageType.start,  "1");

                        sendMessage(message, true);

                        Message attackMessage = new Message(Message.MessageType.attack, "1");

                        sendMessage(attackMessage, true);
                    }

                    continue;
                }

                boolean isFirstTurn = state == GameState.firstTurn;


                Message message = (isFirstTurn ? player1Queue : player2Queue).poll();

                if (message != null && message.getType() == Message.MessageType.attack) {
                    logger.info("Turn of: {}", isFirstTurn ? "First" : "Second");

                    List<List<Point>> field = isFirstTurn ? secondField : firstField;

                    List<String> tokens = Arrays.asList(message.getContent().split(";"));

                    if (tokens.size() != 2) {
                        throw new RuntimeException("Attack message has incorrect content: " + message.getContent());
                    }

                    int x = Integer.parseInt(tokens.get(0));
                    int y = Integer.parseInt(tokens.get(1));

                    boolean hit = field.get(x).get(y).getHasShip() && !field.get(x).get(y).getIsHurt();

                    if (hit) {
                        Message hitMessage = new Message(Message.MessageType.hit, getProcessHitMessage(field, x, y));

                        sendMessage(hitMessage, !isFirstTurn);

                        if (!isFieldAlive(field)) {
                            sendMessage(new Message(Message.MessageType.win, ""), isFirstTurn);
                            state = GameState.end;
                        } else {
                            Message attackMessage = new Message(Message.MessageType.attack, "1");

                            sendMessage(attackMessage, isFirstTurn);
                        }
                    } else {
                        state = state == GameState.firstTurn ? GameState.secondTurn : GameState.firstTurn;
                        Message emptyHit = new Message(Message.MessageType.hit, x + ";" + y + ";" + 1);

                        sendMessage(emptyHit, !isFirstTurn);

                        Message attackMessage = new Message(Message.MessageType.attack, "1");

                        sendMessage(attackMessage, !isFirstTurn);
                    }
                } else {
                    Thread.yield();
                }

            }


        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        } finally {
            closeConnections();
        }
    }

    private String getProcessHitMessage(List<List<Point>> field, int x, int y) {
        StringBuilder str = new StringBuilder();

        field.get(x).get(y).setHurt(true);

        str.append(x).append(";").append(y).append(";").append("2");

        ShipDimension dim = getShipInfo(field, x, y);

        if (!isShipAlive(field, dim)) {
            int startX = dim.startX - 1;
            int startY = dim.startY - 1;
            int endX = (dim.isHorizontal ? dim.startX + 1 : dim.startX + dim.size);
            int endY = (dim.isHorizontal ? dim.startY + dim.size : dim.startY + 1);

            for(int i = startX; i <= endX; ++i) {
                for (int j = startY; j <= endY; ++j) {
                    if (i >= 0 && i <= 9 && j >= 0 && j <= 9) {
                        boolean isInside = (dim.isHorizontal && i == dim.startX && j >= dim.startY && j < dim.startY + dim.size) ||
                                (!dim.isHorizontal && j == dim.startY && i >= dim.startX && i < dim.startX + dim.size);

                        str.append(";").append(i).append(";").append(j).append(";").append(isInside ? "2" : "1");
                    }
                }
            }
        }

        return str.toString();
    }

    private void sendMessage(Message message, boolean toFirst) {
        message.setIsForMe(toFirst);

        out1.write(message.toString());
        out1.flush();

        message.setIsForMe(!toFirst);

        out2.write(message.toString());
        out2.flush();
    }

    private static boolean isShipAlive(List<List<Point>> field, ShipDimension dim) {
        int x = dim.startX;
        int y = dim.startY;

        for(int i = 0; i < dim.size; ++i) {
            int nx = dim.isHorizontal ? x : x + i;
            int ny = dim.isHorizontal ? y + i : y;

            if (!field.get(nx).get(ny).getIsHurt()) {
                return true;
            }
        }
        return false;
    }

    private static boolean isFieldAlive(List<List<Point>> field) {

        for (List<Point> lst : field) {
            for (Point point : lst) {
                if (point.getHasShip() && !point.getIsHurt()) {
                    return true;
                }
            }
        }

        return false;
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

    private boolean fillField(List<List<Point>> field, String data) {
        field.clear();
        if (data.length() != 100) {
            logger.error("Wrong field length");
            return false;
        }

        for (int i = 0; i < 10; i++) {
            List<Point> lst = new ArrayList<>(10);
            for (int j = 0; j < 10; j++) {
                boolean res = data.charAt(i * 10 + j) == '1';
                lst.add(res ? new Point(true) : new Point(false));
            }
            field.add(lst);
        }
        return true;
    }

    private boolean checkField(List<List<Point>> field) {
        Map<Index, Boolean> mappedField = new HashMap<>();

        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                mappedField.put(new Index(i, j), field.get(i).get(j).getHasShip());
            }
        }

        int ship4 = 0, ship3 = 0, ship2 = 0, ship1 = 0;

        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                Index ind = new Index(i, j);
                if (!mappedField.containsKey(ind) || !mappedField.get(ind)) {
                    continue;
                }

                ShipDimension res = getShipInfo(field, i, j);

                switch (res.size) {
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

                for(int k = 0; k < res.size; ++k) {
                    ind.x = res.isHorizontal ? i : i + k;
                    ind.y = res.isHorizontal ? j + k : j;

                    mappedField.remove(ind);
                }
            }
        }

        return ship1 == 4 && ship2 == 3 && ship3 == 2 && ship4 == 1;
    }

    private static class ShipDimension {
        public int size;
        public boolean isHorizontal = false;
        public int startX;
        public int startY;

        public ShipDimension(int size) {
            this.size = size;
        }

        public ShipDimension(int size, boolean isHorizontal, int startX, int startY) {
            this.size = size;
            this.isHorizontal = isHorizontal;
            this.startX = startX;
            this.startY = startY;
        }
    }

    private ShipDimension getShipInfo(List<List<Point>> field, int x, int y) {
        int res = 1;

        int minx = x, maxx = x;
        int miny = y, maxy = y;
        boolean needSearch = true;

        Index ind = new Index(x, y);

        while (minx > 0 && needSearch) {
            --minx;
            ind.x = minx;
            if (!field.get(ind.x).get(ind.y).getHasShip()) {
                ++minx;
                needSearch = false;
            }
        }

        needSearch = true;

        while (maxx < 9 && needSearch) {
            ++maxx;
            ind.x = maxx;
            if (!field.get(ind.x).get(ind.y).getHasShip()) {
                --maxx;
                needSearch = false;
            }
        }

        needSearch = true;
        ind.x = x;

        while (miny > 0 && needSearch) {
            --miny;
            ind.y = miny;
            if (!field.get(ind.x).get(ind.y).getHasShip()) {
                ++miny;
                needSearch = false;
            }
        }

        needSearch = true;

        while (maxy < 9 && needSearch) {
            ++maxy;
            ind.y = maxy;
            if (!field.get(ind.x).get(ind.y).getHasShip()) {
                --maxy;
                needSearch = false;
            }
        }

        if(maxy - miny != 0 && maxx - minx != 0) {
            return new ShipDimension(0);
        }

        boolean isHorizontal = maxy - miny != 0;

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

                if (field.get(ind.x).get(ind.y).getHasShip() &&
                    !((ind.x >= minx && ind.x <= maxx) && (ind.y >= miny && ind.y <= maxy))) {
                    return new ShipDimension(0);
                }
            }
        }

        return new ShipDimension(res, isHorizontal, minx, miny);
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
                        } else {
                            logger.info("Received message from {}", playerName);
                            queue.put(message);
                        }
                    } else {
                        Thread.yield();
                    }

                    Long newTime = System.currentTimeMillis();

                    diffBeat += newTime - previousTime;
                    previousTime = newTime;

                    if (diffBeat > MAX_BEAT_WAIT) {
                        if (wasHeart) {
                            throw new RuntimeException(playerName + " disconnected");
                        } else {
                            out.write((new Message(Message.MessageType.heart, "1").toString()));
                            out.flush();
                            wasHeart = true;
                            diffBeat = 0L;
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("{} broke connection: {}", playerName, e.getMessage());
                disconnected.set(true);
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
