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

    private boolean myTurn;
    private boolean isNotClosed = true;

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
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        try {
            while (!server.isClosed() && server.isConnected()) {
                Object obj = objectInputStream.readObject();
                if (obj instanceof Message message) {
                    switch (message.getMessageType()) {
                        case CONNECT -> {
                            clientId = message.getClientID();
                            opponentID = clientId % 2 == 0 ? clientId + 1 : clientId - 1;
                        }
                        case START_FILLING -> Platform.runLater(() -> clientController.setShipFillingIsStarted());
                        case WIN -> Platform.runLater(() -> clientController.setWin());
                        case DEFEAT -> Platform.runLater(() -> clientController.setDefeat());
                        case GAME_BEGIN -> Platform.runLater(() -> clientController.beginGame());
                        case MY_TURN -> Platform.runLater(() -> clientController.setMyTurn());
                        case ENEMY_TURN -> Platform.runLater(() -> clientController.setEnemyTurn());
                    }
                } else if (obj instanceof TurnInfo turnInfo ){
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
            throw new RuntimeException(e);
        }
    }

    public void clientCloseConnection() {
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
    }

    public void writeToServer(Message message) throws IOException {
        objectOutputStream.writeObject(message);
    }

    public void writeToServer(TurnInfo turnInfo) throws IOException {
        objectOutputStream.writeObject(turnInfo);
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    public int getClientId() {
        return clientId;
    }

    public int getOpponentID() {
        return opponentID;
    }

    public boolean getMyTurn() {
        return myTurn;
    }

    private boolean checkEnemyHit(Message message) {
        return true;
    }
}
