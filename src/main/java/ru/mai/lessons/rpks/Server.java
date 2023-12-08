package ru.mai.lessons.rpks;

import org.apache.log4j.Logger;
import ru.mai.lessons.rpks.clients.Message;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Server {

    private static final Logger logger = Logger.getLogger(Server.class.getName());

    private String host = "localhost"; //127.0.0.1
    private Integer port = 8843;

    private boolean isTurn = true;

    private HashMap<Integer, Boolean> clientsAreReady = new HashMap<>();
    private List<ClientHandler> clients = new ArrayList<>();

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
            while (true) {
                Socket client = serverSocket.accept();
                logger.info("Подключился новый клиент: " + client.toString());
                ClientHandler clientHandler;
                clientHandler = new ClientHandler(client, this, clients.size() + 1, isTurn);
                isTurn = !isTurn;
                clients.add(clientHandler);
                clientsAreReady.put(clients.size(), Boolean.FALSE);
                new Thread(clientHandler).start();
            }
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

    public void setClientIsReady(int clientID) {
        clientsAreReady.put(clientID, Boolean.TRUE);
    }

    public Message waitUntilTwoReady(int clientID) {
        int opponent = clientID % 2 == 1 ? clientID + 1 : clientID - 1;
        while (!clientsAreReady.get(opponent)) {

        }
        return new Message(clientID, Message.MessageType.GAME_BEGIN);
    }

    public void sendMessageToOpponent(Message message) {
        if (message.getMessageType() == Message.MessageType.TURN_INFO) {
            message.setMessageType(Message.MessageType.ENEMY_TURN_INFO);
        }
        if (message.getClientID() % 2 == 0) {
            clients.get(message.getClientID() - 1).sendMessage(message);
        } else {
            clients.get(message.getClientID() + 1).sendMessage(message);
        }
    }
}
