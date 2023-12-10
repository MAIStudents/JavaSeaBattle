package ru.mai.lessons.rpks.clients;

import java.io.Serializable;

public class Message implements Serializable {
    private final int clientID;
    private MessageType messageType;

    private Point point;

    public enum MessageType {
        SET_READY,
        GAME_BEGIN,
        START_FILLING,
        MY_TURN,
        ENEMY_TURN,
        WIN,
        DEFEAT,
        CONNECT,
        DISCONNECT,
        ENEMY_DISCONNECTED
    }

    public Message(int clientID, MessageType messageType) {
        this.clientID = clientID;
        this.messageType = messageType;
    }

    public Message(int clientID, Message.MessageType messageType, Point point)
    {
        this.clientID = clientID;
        this.messageType = messageType;
        this.point = point;
    }

    public int getClientID( ) {
        return clientID;
    }
    public MessageType getMessageType() {
        return messageType;
    }
    public Point getPoint() {return point;}

    public void setMessageType(MessageType type) {
        this.messageType = type;
    }

    @Override
    public String toString() {
        if (point != null) {
            return "Message{" +
                    "clientID=" + clientID + "," +
                    "messageType=" + messageType.name() + "," +
                    point + "}";
        }
        return "Message{" +
                "clientID=" + clientID + "," +
                "messageType=" + messageType.name() + "}";
    }



}
