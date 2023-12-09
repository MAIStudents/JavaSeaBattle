package ru.mai.lessons.rpks;

import org.apache.log4j.Logger;
import ru.mai.lessons.rpks.clients.Message;
import ru.mai.lessons.rpks.clients.TurnInfo;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());

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
            objectOutputStream.writeObject(new Message(clientID, Message.MessageType.CONNECT));
            if (clientID % 2 == 1) {
                server.sendMessageToOpponent(new Message(clientID, Message.MessageType.START_FILLING));
                objectOutputStream.writeObject(new Message(clientID, Message.MessageType.START_FILLING));
            }

            while (!client.isClosed() && client.isConnected()) {
                Object obj = objectInputStream.readObject();
                if (obj instanceof Message message) {
                    if (message.getMessageType() == Message.MessageType.SET_READY) {
                        server.setClientIsReady(clientID);
                        objectOutputStream.writeObject(server.waitUntilTwoReady(clientID));
                        if (isTurn) {
                            objectOutputStream.writeObject(new Message(clientID, Message.MessageType.MY_TURN));
                        } else {
                            objectOutputStream.writeObject(new Message(clientID, Message.MessageType.ENEMY_TURN));
                        }
                    } else {
                        server.sendMessageToOpponent(message);
                    }
                } else if (obj instanceof TurnInfo turnInfo) {
                    if (turnInfo.getType() == TurnInfo.TurnType.MISS) {
                        objectOutputStream.writeObject(new Message(clientID, Message.MessageType.MY_TURN));
                        server.sendMessageToOpponent(new Message(clientID, Message.MessageType.ENEMY_TURN));
                    }
                    server.SendTurnInfoToOpponent(turnInfo);
                }
            }
        } catch (IOException e) {
            logger.error("Ошибка при работе с клиентом", e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendTurnInfo(TurnInfo turnInfo) {
        try {
            objectOutputStream.writeObject(turnInfo);
            objectOutputStream.flush();
        } catch (IOException e) {
            logger.error("Проблема при записи сообщения в поток клиента: " + client.toString(), e);
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
