package ru.mai.lessons.rpks.clients;

import java.io.Serializable;

public class Message implements Serializable {
    private int clientID;
    private String message;
    private MessageType messageType;

    private int row;
    private int col;

    public enum MessageType {
        SET_READY,
        GAME_BEGIN,
        MY_TURN,
        ENEMY_TURN,
        TURN_INFO,
        ENEMY_TURN_INFO,
        TURN_MISSED,
        TURN_HIT,
        DISCONNECT

    }

    public Message(int clientID, String message, MessageType messageType) {
        this.clientID = clientID;
        this.message = message;
        this.messageType = messageType;
        this.row = -1;
        this.col = -1;
    }

    public Message(int clientID, String message, Message.MessageType messageType, int row, int col)
    {
        this.clientID = clientID;
        this.message = message;
        this.messageType = messageType;
        this.row = row;
        this.col = col;
    }

    public int getClientID( ) {
        return clientID;
    }
    public String getMessage() {
        return message;
    }
    public MessageType getMessageType() {
        return messageType;
    }
    public int getRow() {return row;}
    public int getColumn() {return col;}

    public void setMessageType(MessageType type) {
        this.messageType = type;
    }

    @Override
    public String toString() {
        return "Message{" +
                "clientID=" + clientID + "," +
                "message=" + message +"," +
                "messageType=" + messageType.name() + "," +
                "row=" + row + "," +
                "col=" + col +"}";
    }

}
