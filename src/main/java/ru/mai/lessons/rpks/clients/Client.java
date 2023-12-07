package ru.mai.lessons.rpks.clients;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;


public class Client extends Application {

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

    private int[][] clientGrid;

    public Client() {
    }

    public Client(String host, Integer port) {
        this.host = host;
        this.port = port;
        this.myTurn = false;
    }

    public void start() {

        try {
            server = new Socket(host, port);
            objectOutputStream = new ObjectOutputStream(server.getOutputStream());
            objectInputStream = new ObjectInputStream(server.getInputStream());
            logger.info("Клиент инициализирован");

            try {
                startGame();
                while (!server.isClosed() && server.isConnected()) {
                    Message message = (Message) objectInputStream.readObject();
                    switch (message.getMessageType()) {
                        case GAME_BEGIN -> {
                            // todo: set not visible rules for arrangement, remove ready btn, enable enemy grid (mouse clicking) on action
                            Platform.runLater(() -> clientController.beginGame());
                        }
                        case MY_TURN -> {
                            myTurn = true;
                            // todo: set label
                        }
                        case ENEMY_TURN -> {
                            myTurn = false;
                            // todo: set label
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
//            finally {
//                if (objectInputStream != null) objectInputStream.close();
//                try {
//                    server.close();
//                } catch (IOException ex) {
//                    logger.error("Проблема при закрытии соединения с сервером", ex);
//                }
//            }

            while (isNotClosed) {

            }
            objectOutputStream.close();
        } catch (IOException e) {
            logger.error("Ошибка при подключении к серверу", e);
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

    }

    private void startGame() {
        Platform.runLater(() -> {
            try {
                Stage stageClient = new Stage();
                clientController = new ClientController(this);
                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("client.fxml"));
                fxmlLoader.setController(clientController);
                Scene scene = new Scene(fxmlLoader.load());
                stageClient.setResizable(false);
                stageClient.setTitle("Sea Battle");
                stageClient.setScene(scene);
                stageClient.show();
                stageClient.setOnCloseRequest(event -> clientCloseConnection());
            } catch (IOException ex) {
                logger.error("Ошибка при создании сцены для клиента", ex);
            }
        });
    }

    public void clientCloseConnection() {
        try {
            objectOutputStream.writeObject(new Message(clientId, "", Message.MessageType.DISCONNECT));
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
