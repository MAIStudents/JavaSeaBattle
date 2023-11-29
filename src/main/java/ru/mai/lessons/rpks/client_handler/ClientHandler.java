package ru.mai.lessons.rpks.client_handler;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import org.apache.log4j.Logger;
import ru.mai.lessons.rpks.client.ClientController;
import ru.mai.lessons.rpks.message.Message;
import ru.mai.lessons.rpks.server.Server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler {
    private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());
    private Socket client;
    private Server server;
    private int clientId;

    public ClientHandler(Socket client, Server server, int clientId) {
        this.client = client;
        this.server = server;
        this.clientId = clientId;
    }

    public void handle() {
        try (ObjectInputStream objectInputStream = new ObjectInputStream(client.getInputStream());
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(client.getOutputStream())) {
            while (server.isAlive() && !client.isClosed()) {
                Message message = (Message) objectInputStream.readObject();
                logger.info(message);

                switch (message.getContent()) {
                    case "START_GAME" -> startGame();
                    case "CHECK_CONNECT" -> objectOutputStream.writeObject(checkConnect());
                    case "ERROR_CONNECT" -> serverFull();
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            if (server.isAlive() && !client.isClosed()) {
                logger.error("Ошибка при работе с клиентом", e);
            }
        } finally {
            closeConnection();
        }
    }

    public void closeConnection() {
        if (client != null) {
            try {
                client.close();
            } catch (IOException ex) {
                logger.error("Ошибка при закрытии клиента!", ex);
            }
        }
    }

    public void startGame() {
        Platform.runLater(() -> {
            try {
                Stage stageClient = new Stage();
                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("client-view.fxml"));
                fxmlLoader.setController(new ClientController(clientId));
                Scene scene = new Scene(fxmlLoader.load(), 800, 600);
                stageClient.setResizable(false);
                stageClient.setTitle("Sea Battle");
                stageClient.setScene(scene);
                stageClient.show();
                stageClient.setOnCloseRequest(event -> {
                    closeConnection();
                    server.clientDisconnect();
                });
            } catch (IOException ex) {
                logger.error("Ошибка при создании сцены для клиента", ex);
            }
        });
    }

    private Message checkConnect() {
        Message message = null;

        if (clientId >= 2) {
            message = new Message("server", "ERROR");
        } else {
            message = new Message("server", "SUCCESS");
        }

        return message;
    }

    private void serverFull() {
        closeConnection();

        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Сервер забит");
            alert.showAndWait();
        });
    }
}