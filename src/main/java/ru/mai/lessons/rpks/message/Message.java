package ru.mai.lessons.rpks.message;

import ru.mai.lessons.rpks.point.Point;

import java.io.Serializable;

public class Message implements Serializable {
    public static String[] message_types = new String[] {
            "SUCCESS",
            "START_GAME",
            "CHECK_CONNECT",
            "ERROR_CONNECT",
            "DISCONNECT",
            "READY",
            "ENEMY_CONNECTED",
            "ENEMY_READY",
            "GAME_BEGIN"
    };

    private int senderId;
    private String content;
    private String messageType;
    private int row;
    private int col;

    public Message(int senderId, String content, String messageType) {
        this.senderId = senderId;
        this.content = content;
        this.messageType = messageType;
        this.row = 0;
        this.col = 0;
    }

    public Message(int senderId, String content, String messageType, int row, int col) {
        this.senderId = senderId;
        this.content = content;
        this.messageType = messageType;
        this.row = row;
        this.col = col;
    }

    public int getSenderId() {
        return senderId;
    }

    public String getContent() {
        return content;
    }

    public String getMessageType() {
        return messageType;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    @Override
    public String toString() {
        return senderId + ": " + content + "[" + messageType + "]";
    }
}
