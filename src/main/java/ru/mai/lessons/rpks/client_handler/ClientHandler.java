package ru.mai.lessons.rpks.client_handler;

import org.apache.log4j.Logger;
import ru.mai.lessons.rpks.battle_grid.BattleGrid;
import ru.mai.lessons.rpks.message.Message;
import ru.mai.lessons.rpks.server.Server;

import java.io.*;
import java.net.Socket;

public class ClientHandler {
    private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());
    private final Socket socketForClient;
    private final Server server;
    private final int clientId;
    private boolean isReady = false;
    private BattleGrid battleGridMain;
    private final boolean labelSet = false;
    private final ObjectOutputStream objectOutputStream;
    private final ObjectInputStream objectInputStream;

    public ClientHandler(Socket socketForClient, OutputStream outputStream, InputStream inputStream, Server server, int clientId) throws IOException {
        this.socketForClient = socketForClient;
        this.objectOutputStream = new ObjectOutputStream(outputStream);
        this.objectInputStream = new ObjectInputStream(inputStream);
        this.server = server;
        this.clientId = clientId;
    }

    public void handle() {
        try {
            while (server.isAlive() && !socketForClient.isClosed()) {
                Message message = (Message) objectInputStream.readObject();
                logger.info(message);

                switch (message.getMessageType()) {
                    case "CHECK_CONNECT" -> objectOutputStream.writeObject(checkConnect());
                    case "ERROR_CONNECT" -> logger.error("Сервер переполнен");
                    case "DISCONNECT" -> {
                        server.clientDisconnect(clientId);
                        closeConnection();
                    }
                    case "READY" -> {
                        isReady = true;
                        battleGridMain = getBattleGrid();
                        battleGridMain.outputBattleGrid();
                        logger.info("Клиент готов к игре");

                        if (getEnemy() != null && getEnemy().isReady()) {
                            getEnemy().sendSimpleMessage("", "GAME_BEGIN");
                            sendSimpleMessage("", "GAME_BEGIN");
                        }
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            if (server.isAlive() && !socketForClient.isClosed()) {
                logger.error("Ошибка при работе с клиентом", e);
            }
        } finally {
            closeConnection();
        }
    }

    public void closeConnection() {
        if (socketForClient != null) {
            try {
                objectOutputStream.close();
                objectInputStream.close();
                socketForClient.close();
            } catch (IOException ex) {
                logger.error("Ошибка при закрытии клиента!", ex);
            }
        }
    }

    private Message checkConnect() {
        Message message = null;

        if (clientId >= 3) {
            message = new Message(0, Integer.toString(clientId), "ERROR_CONNECT");
        } else {
            message = new Message(0, Integer.toString(clientId), "SUCCESS");
        }

        return message;
    }

    public int getClientId() {
        return clientId;
    }

    public boolean isReady() {
        return isReady;
    }

    public ClientHandler getEnemy() {
        for (ClientHandler client : server.getClients()) {
            if (client.getClientId() != clientId) {
                return client;
            }
        }

        return null;
    }

    public void sendSimpleMessage(String content, String messageTYpe) {
        try {
            objectOutputStream.writeObject(new Message(0, content, messageTYpe));
            objectOutputStream.flush();
        } catch (IOException ex) {
            logger.error("Ошибка при отправке сообщения сервером", ex);
        }
    }

    public BattleGrid getBattleGrid() {
        try {
            sendSimpleMessage("", "GET_BATTLE_GRID");
            return (BattleGrid) objectInputStream.readObject();
        } catch (IOException ex) {
            logger.error("Ошибка при записи сообщения сервером", ex);
        } catch (ClassNotFoundException ex) {
            logger.error("Ошибка при получении сообщения сервером", ex);
        }

        return null;
    }
}