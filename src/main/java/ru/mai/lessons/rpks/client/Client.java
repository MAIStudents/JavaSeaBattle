package ru.mai.lessons.rpks.client;

import javafx.scene.control.Alert;
import org.apache.log4j.Logger;
import ru.mai.lessons.rpks.message.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;


public class Client {
    private static final Logger logger = Logger.getLogger(Client.class.getName());
    private String host = "localhost";
    private Integer port = 8843;
    private ObjectOutputStream objectOutputStream;
    private ObjectInputStream objectInputStream;

    public Client() {
    }

    public Client(String host, Integer port) {
        this.host = host;
        this.port = port;
    }

    public void start() {
        try (Socket server = new Socket(host, port)) {
            logger.info("Клиент инициализирован");
            objectOutputStream = new ObjectOutputStream(server.getOutputStream());
            objectInputStream = new ObjectInputStream(server.getInputStream());

            if (checkConnect()) {
                objectOutputStream.writeObject(new Message("client", "START_GAME"));
                objectOutputStream.flush();

                try {
                    while (true) {
                        Message message = (Message) objectInputStream.readObject();
                        logger.info(message);
                    }
                } catch (IOException | ClassNotFoundException ex) {
                    if (!server.isConnected()) {
                        logger.error("Проблема при чтении сервера клиентом", ex);
                    }
                }
            } else {
                objectOutputStream.writeObject(new Message("client", "ERROR_CONNECT"));
                objectOutputStream.flush();
            }
        } catch (IOException ex) {
            logger.error("Ошибка при подключении к серверу", ex);
        }
    }

    private boolean checkConnect() {
        try {
            objectOutputStream.writeObject(new Message("client", "CHECK_CONNECT"));
            objectOutputStream.flush();
            Message response = (Message) objectInputStream.readObject();
            logger.info(response);

            if (response.getContent().equals("SUCCESS")) {
                return true;
            }
        } catch (IOException ex) {
            logger.error("Ошибка при записи сообщения клиентом", ex);
        } catch (ClassNotFoundException ex) {
            logger.error("Ошибка при чтении сообщения клиентом", ex);
        }

        return false;
    }
}