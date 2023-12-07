package ru.mai.lessons.rpks;

import org.apache.log4j.Logger;
import ru.mai.lessons.rpks.Server;
import ru.mai.lessons.rpks.clients.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ClientHandler implements Runnable {

    private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());

    private static final String ANCHOR_NAME = "###";

    private Socket client;
    private Server server;
    private int clientID;
    private boolean isTurn;

    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;


    public ClientHandler(Socket client, Server server, int clientID, boolean isTurn) {
        this.client = client;
        this.server = server;
        this.clientID = clientID;
        this.isTurn = isTurn;
    }

    @Override
    public void run() {
        try {
            objectInputStream = new ObjectInputStream(client.getInputStream());
            objectOutputStream = new ObjectOutputStream(client.getOutputStream());
            sendMessage(new Message(clientID, "", Message.MessageType.MY_TURN));

            while (!client.isClosed() && client.isConnected()) {
                Message message = (Message) objectInputStream.readObject();
                if (message.getMessageType() == Message.MessageType.SET_READY) {

                } else {
                    server.sendMessageToOpponent(message);
                }
            }
        } catch (IOException e) {
            logger.error("Ошибка при работе с клиентом", e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            if (objectInputStream != null) {
                try {
                    objectInputStream.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            if (client != null) {
                try {
                    client.close();
                } catch (IOException ex) {
                    logger.error("Ошибка при закрытии клиента!", ex);
                }
            }
        }
    }

    public void sendMessage(Message message) {
        try {
            objectOutputStream.writeObject(message);
            objectOutputStream.flush();
        } catch (IOException e) {
            logger.error("Проблема при записи сообщения в поток клиента: " + client.toString(), e);
        }
    }
}
