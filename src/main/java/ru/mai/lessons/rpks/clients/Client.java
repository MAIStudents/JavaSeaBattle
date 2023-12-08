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
    private Socket server;

    private boolean myTurn;
    private boolean isNotClosed = true;

    private ClientController clientController;

    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;

    public Client() {
        try {
            server = new Socket(host, port);
            objectOutputStream = new ObjectOutputStream(server.getOutputStream());
            objectInputStream = new ObjectInputStream(server.getInputStream());
            logger.info("Клиент инициализирован");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Client(String host, Integer port) {
        this.host = host;
        this.port = port;
        this.myTurn = false;
    }

    @Override
    public void run() {
        try {
            try {
                while (!server.isClosed() && server.isConnected()) {
                    Message message = (Message) objectInputStream.readObject();
                    switch (message.getMessageType()) {
                        case CONNECT -> {
                            clientId = message.getClientID();
                        }
                        case WIN -> {
                            clientController.disactivateOpponentField();
                            clientController.labelAdditionalInfoSetSuccessMessage("Вы выиграли! =)");
                        }
                        case DEFEAT -> {
                            clientController.disactivateOpponentField();
                            clientController.labelAdditionalInfoSetSuccessMessage("Вы проиграли! =(");
                        }
                        case GAME_BEGIN -> {
                            // todo: set not visible rules for arrangement, remove ready btn, enable enemy grid (mouse clicking) on action
                            Platform.runLater(() -> clientController.beginGame());
                        }
                        case MY_TURN -> {
                            myTurn = true;
                            clientController.activateOpponentField();
                            clientController.labelTurnSetMyTurn();
                        }
                        case ENEMY_TURN -> {
                            myTurn = false;
                            clientController.disactivateOpponentField();
                            clientController.labelTurnSetEnemyTurn();
                        }
                        case ENEMY_TURN_INFO -> {
                            objectOutputStream.writeObject(checkEnemyHit(message));
                        }
                        case TURN_MISSED -> {
                            clientController.markMissedTurn(message);
                        }
                        case TURN_HIT -> {
                            clientController.markHitTurn(message);
                        }
                    }
                }
            } catch (IOException e) {
                logger.error("Проблема при чтении сервера клиентом", e);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }

            objectOutputStream.close();
        } catch (IOException e) {
            logger.error("Ошибка при подключении к серверу", e);
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

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    public int getClientId() {
        return clientId;
    }

    public boolean getMyTurn() {
        return myTurn;
    }

    private boolean checkEnemyHit(Message message) {
        return true;
    }
}
