package ru.mai.lessons.rpks.Server;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

import static ru.mai.lessons.rpks.Components.Responses.*;

import ru.mai.lessons.rpks.Client.ClientHandler;
import ru.mai.lessons.rpks.Components.InitPoints;

public class Server {

    private static final Logger logger = Logger.getLogger(Server.class.getName());
    private String host = "127.0.0.1"; //127.0.0.1
    private Integer port = 8843;
    private ClientHandler playerOne;
    private ClientHandler playerTwo;

    private Map<InitPoints, Boolean> firstPlayerField;
    private Map<InitPoints, Boolean> secondPlayerField;

    public Server() {
    }

    public Server(String host, Integer port) {
        this.host = host;
        this.port = port;
    }

    public void start() {
        logger.info("Инициализация сервера");
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(port);
            logger.info("Сервер стартовал и ожидает подключение клиента");
            while (playerTwo == null) {
                Socket client = serverSocket.accept();
                logger.info("Подключился новый клиент: " + client.toString());
                ClientHandler clientHandler = new ClientHandler(client, this);
                addPlayer(clientHandler);
                new Thread(clientHandler).start();
            }

            logger.info("Игра началась");
            game();
            logger.info("Конец игры");

        } catch (IOException e) {
            logger.error("Проблема с сервером", e);
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException ex) {
                    logger.error("Проблема при закрытии сервера", ex);
                }
            }
        }
    }

    private boolean gameStart() {
        return playerTwo != null;
    }

    private void game() {

        ClientHandler turnClient = playerOne;
        boolean needSwitch = true;

        for (int i = 0; playersOnline() && i < 2; ++i) {

            turnClient.sendMessage(TURN);

            String turn = turnClient.getClientMessage();

            initializeField(turn, turnClient);

            turnClient = switchClient(turnClient);
        }

        while (playersOnline()) {

            if (needSwitch) {
                turnClient.sendMessage(TURN);
            }

            String turn = turnClient.getClientMessage();

            if (turn.equals(LEAVE)) {
                removeClient(turnClient);
                break;
            }

            needSwitch = false;
            String response = getShotResponse(turnClient, turn);

            if (response.equals(PAST)) {
                needSwitch = true;
            } else if (response.equals(WIN)) {

                turnClient.sendMessage(WIN);
                switchClient(turnClient).sendMessage(OPPONENT_TURN + " " + turn);
                switchClient(turnClient).sendMessage(LOSE);

                removeAllClients();
                break;
            }

            turnClient.sendMessage(response);
            if (switchClient(turnClient) != null) {
                switchClient(turnClient).sendMessage(OPPONENT_TURN + " " + turn);
            }

            if (needSwitch) {
                turnClient = switchClient(turnClient);
            }
        }

        if (playerOne != null) {
            playerOne.sendMessage(OPPONENT_LEFT);
        }
    }

    private void initializeField(String message, ClientHandler client) {

        Map<InitPoints, Boolean> field = new HashMap<>();

        String[] positions = message.split("#");

        logger.debug("String init field: " + message);

        int x0, x1, y0, y1;

        for (int i = 0; i < positions.length; ++i) {

            String[] coordinates = positions[i].split(" ");
            x0 = Integer.parseInt(coordinates[0]);
            y0 = Integer.parseInt(coordinates[1]);
            x1 = Integer.parseInt(coordinates[2]);
            y1 = Integer.parseInt(coordinates[3]);

            logger.debug("coordinates");
            logger.debug(x0 + " " + y0 + " " + x1 + " " + y1);


            field.put(new InitPoints(x0, y0), true);

            if (x0 == x1) {
                for (int j = y0 + 1; j <= y1; ++j) {
                    field.put(new InitPoints(x0, j), true);
                }
            } else {
                for (int j = x0 + 1; j <= x1; ++j) {
                    field.put(new InitPoints(j, y0), true);
                }
            }
        }

        if (client.equals(playerOne)) {
            firstPlayerField = field;
        } else {
            secondPlayerField = field;
        }
    }

    private ClientHandler switchClient(ClientHandler player) {
        if (player.equals(playerOne)) {
            return playerTwo;
        }
        return playerOne;
    }

    private boolean playersOnline() {
        return playerTwo != null;
    }

    private void addPlayer(ClientHandler player) {
        if (playerOne == null) {
            playerOne = player;
        } else {
            playerTwo = player;
        }
    }

    private void removeAllClients() {
        while (playerOne != null) {
            removeClient(playerOne);
        }
    }

    public void removeClient(ClientHandler player) {
        if (playerOne == null) return;
        if (playerOne.equals(player)) {
            playerOne = playerTwo;
        }
        playerTwo = null;
    }

    private String getShotResponse(ClientHandler client, String turn) {

        String[] coordinates = turn.split(" ");

        InitPoints point = new InitPoints(Integer.parseInt(coordinates[0]), Integer.parseInt(coordinates[1]));

        Map<InitPoints, Boolean> field;
        if (client.equals(playerOne)) {
            field = secondPlayerField;
        } else {
            field = firstPlayerField;
        }


        if (field.containsKey(point)) {

            field.put(point, false);

            int nextX, nextY, dx, dy;

            List<InitPoints> directions = new ArrayList<>(
                    Arrays.asList(
                            new InitPoints(0, 1),
                            new InitPoints(0, -1),
                            new InitPoints(1, 0),
                            new InitPoints(-1, 0)
                    )
            );


            for (int i = 0; i < directions.size(); ++i) {

                nextX = point.getX();
                nextY = point.getY();

                dx = directions.get(i).getX();
                dy = directions.get(i).getY();

                while (field.containsKey(new InitPoints(nextX, nextY))) {
                    if (field.get(new InitPoints(nextX, nextY))) {
                        return WOUNDED;
                    }
                    nextX += dx;
                    nextY += dy;
                }
            }

            if (field.containsValue(true)) {
                return KILLED;
            }

            return WIN;
        }
        return PAST;
    }
}