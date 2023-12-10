package ru.mai.lessons.rpks.clients;

import javafx.application.Platform;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;


public class Client implements Runnable {

    private static final Logger logger = Logger.getLogger(Client.class.getName());

    private String host = "localhost";
    private Integer port = 8843;
    private int clientId;
    private int opponentID;
    private Socket server;

    private ClientController clientController;

    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;

    public Client(ClientController clientController) {
        try {
            this.clientController = clientController;
            server = new Socket(host, port);
            objectOutputStream = new ObjectOutputStream(server.getOutputStream());
            objectInputStream = new ObjectInputStream(server.getInputStream());
            logger.info("Клиент инициализирован");
        } catch (IOException e) {
            logger.error("Ошибка при подключении к серверу", e);
            Platform.exit();
        }
    }

    public Client(String host, Integer port, ClientController clientController) {
        this.host = host;
        this.port = port;
        this.clientController = clientController;
        try {
            server = new Socket(host, port);
            objectOutputStream = new ObjectOutputStream(server.getOutputStream());
            objectInputStream = new ObjectInputStream(server.getInputStream());
            logger.info("Клиент инициализирован");
        } catch (IOException e) {
            logger.error("Ошибка при попытке подключения к серверу", e);
        }
    }

    @Override
    public void run() {
        try {
            while (!server.isClosed() && server.isConnected()) {
                Object obj = objectInputStream.readObject();
                if (obj instanceof Message message) {
                    logger.debug("Сообщение от сервера: " + message);
                    switch (message.getMessageType()) {
                        case CONNECT -> {
                            clientId = message.getClientID();
                            opponentID = clientId % 2 == 0 ? clientId + 1 : clientId - 1;
                        }
                        case START_FILLING -> Platform.runLater(() -> clientController.setShipFillingIsStarted());
                        case WIN -> {
                            clientCloseConnection();
                            Platform.runLater(() -> clientController.setWin());}
                        case DEFEAT -> {
                            clientCloseConnection();
                            Platform.runLater(() -> clientController.setDefeat());
                        }
                        case GAME_BEGIN -> Platform.runLater(() -> clientController.beginGame());
                        case MY_TURN -> Platform.runLater(() -> clientController.setMyTurn());
                        case ENEMY_TURN -> Platform.runLater(() -> clientController.setEnemyTurn());
                        case ENEMY_DISCONNECTED -> Platform.runLater(() -> clientController.stopTheGame());
                    }
                } else if (obj instanceof TurnInfo turnInfo ){
                    logger.debug("Информация о ходе от сервера: " + turnInfo);
                    switch (turnInfo.getType()) {
                        case WRONG ->
                                Platform.runLater(() -> clientController.labelAdditionalInfoSetErrorMessage("Неправильно выбрана клетка для атаки!"));
                        case MISS, HIT -> Platform.runLater(() -> clientController.updateShipCell(turnInfo));
                        case SUNKEN ->
                                Platform.runLater(() -> {
                                    clientController.updateShipCell(new TurnInfo(turnInfo.getClientID(), turnInfo.getPoint(), TurnInfo.TurnType.HIT));
                                    clientController.markNeighborsOfEnemySunkenShip(turnInfo.getListPoint());
                                });
                        case ATTACK -> Platform.runLater(() -> {
                            try {
                                TurnInfo response =  clientController.getResponseToEnemyAttack(turnInfo);
                                objectOutputStream.writeObject(response);
                                if (response.getType() == TurnInfo.TurnType.HIT ||
                                        response.getType() == TurnInfo.TurnType.SUNKEN) {
                                    if (clientController.allShipsAreDestroyed()) {
                                        objectOutputStream.writeObject(new Message(clientId, Message.MessageType.WIN));
                                    }
                                }
                            } catch (IOException e) {
                                logger.error("Проблема при записи серверу", e);
                            }
                        });
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Проблема при чтении сервера клиентом", e);
        } catch (ClassNotFoundException e) {
            logger.error("Ошибка при чтении сообщения от сервера", e);
        } finally {
            clientCloseConnection();
        }
    }

    public void clientCloseConnection() {
        if (server.isClosed()) {
            return;
        }
        try {
            objectOutputStream.writeObject(new Message(clientId, Message.MessageType.DISCONNECT));
            objectOutputStream.flush();
        } catch (IOException ex) {
            logger.error("Ошибка при записи сообщения клиента", ex);
        }

        if (server != null) {
            try {
                objectOutputStream.close();
                objectInputStream.close();
                server.close();
            } catch (IOException ex) {
                logger.error("Ошибка при закрытии клиента!", ex);
            }
        }
        logger.info("Связь клиента с сервером закрыта");
    }

    public void writeToServer(Object message) {
        try {
            objectOutputStream.writeObject(message);
        } catch (IOException e) {
            logger.error("Проблема при записи сообщения в поток сервера: " + server.toString(), e);
        }
    }

    public int getClientId() {
        return clientId;
    }

    public int getOpponentID() {
        return opponentID;
    }
}
