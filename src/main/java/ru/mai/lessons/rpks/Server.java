package ru.mai.lessons.rpks;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Server {

    private static final Logger logger = Logger.getLogger(Server.class.getName());
    private String host = "localhost"; //127.0.0.1
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

    private void game() {

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

    private boolean playersOnline() {
        return playerTwo != null;
    }
    private void addPlayer(ClientHandler player) {
        if(player == null) {
            playerOne = player;
        } else {
            playerTwo = player;
        }
    }
}