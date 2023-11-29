package ru.mai.lessons.rpks.client;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import org.apache.log4j.Logger;
import ru.mai.lessons.rpks.battle_grid.BattleGrid;
import ru.mai.lessons.rpks.message.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class Client {
    private static final Logger logger = Logger.getLogger(Client.class.getName());
    private String host = "localhost";
    private Integer port = 8843;
    private int clientId;
    private Socket socketForServer;
    private ObjectOutputStream objectOutputStream;
    private ObjectInputStream objectInputStream;
    private ClientController clientController;
    private BattleGrid battleGrid;
    private boolean isReady;


    public Client() {
    }

    public Client(String host, Integer port) {
        battleGrid = new BattleGrid();
        this.host = host;
        this.port = port;
        this.clientController = null;
    }

    public void start() {
        try {
            socketForServer = new Socket(host, port);
            objectOutputStream = new ObjectOutputStream(socketForServer.getOutputStream());
            objectInputStream = new ObjectInputStream(socketForServer.getInputStream());
            logger.info("Клиент инициализирован");

            try {
                if (checkConnect(objectOutputStream, objectInputStream)) {
                    startGame();
                    objectOutputStream.writeObject(new Message(clientId, "", "START_GAME"));
                    objectOutputStream.flush();

                    while (!socketForServer.isClosed() && socketForServer.isConnected()) {
                        Message message = (Message) objectInputStream.readObject();

                        switch (message.getMessageType()) {
                            case "GAME_BEGIN" -> Platform.runLater(() -> clientController.beginGame());
                            case "GET_BATTLE_GRID" -> {
                                objectOutputStream.writeObject(battleGrid);
                            }
                        }

                        logger.info(message);
                    }
                } else {
                    logger.error("Сервер переполнен");
                    closeConnection();
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR, "Сервер забит");
                        alert.showAndWait();
                    });
                }
            } catch (ClassNotFoundException | IOException ex) {
                if (!socketForServer.isClosed()) {
                    logger.error("Ошибка при чтении сообщения клиентом", ex);
                }
            }
        } catch (IOException ex) {
            logger.error("Ошибка при подключении к серверу", ex);
        }
    }

    private boolean checkConnect(ObjectOutputStream objectOutputStream, ObjectInputStream objectInputStream) {
        try {
            objectOutputStream.writeObject(new Message(-1, "", "CHECK_CONNECT"));
            objectOutputStream.flush();
            Message response = (Message) objectInputStream.readObject();
            logger.info(response);

            clientId = Integer.parseInt(response.getContent());

            if (response.getMessageType().equals("SUCCESS")) {
                return true;
            }
        } catch (IOException | ClassNotFoundException ex) {
            logger.error("Ошибка при записи сообщения клиентом", ex);
        }

        return false;
    }

    private void startGame() {
        Platform.runLater(() -> {
            try {
                clientController = new ClientController(battleGrid, clientId, this);
                Stage stageClient = new Stage();
                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("client-view.fxml"));
                fxmlLoader.setController(clientController);
                Scene scene = new Scene(fxmlLoader.load(), 800, 600);
                stageClient.setResizable(false);
                stageClient.setTitle("Sea Battle");
                stageClient.setScene(scene);
                stageClient.show();
                stageClient.setOnCloseRequest(event -> closeConnection());
            } catch (IOException ex) {
                logger.error("Ошибка при создании сцены для клиента", ex);
            }
        });
    }

    public void closeConnection() {
        try {
            objectOutputStream.writeObject(new Message(clientId, "", "DISCONNECT"));
            objectOutputStream.flush();
        } catch (IOException ex) {
            logger.error("Ошибка при записи сообщения клиента", ex);
        }

        if (socketForServer != null) {
            try {
                objectOutputStream.close();
                objectInputStream.close();
                socketForServer.close();
            } catch (IOException ex) {
                logger.error("Ошибка при закрытии клиента!", ex);
            }
        }
    }

    public void sendMessage(String content, String messageType) {
        try {
            objectOutputStream.writeObject(new Message(clientId, content, messageType));
            objectOutputStream.flush();
        } catch (IOException ex) {
            logger.error("Ошибка при записи сообщения клиентом");
        }
    }

    public void setReady(boolean ready) {
        isReady = ready;
    }
}