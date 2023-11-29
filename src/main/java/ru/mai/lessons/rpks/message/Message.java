package ru.mai.lessons.rpks.message;

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

    public Message(int senderId, String content, String messageType) {
        this.senderId = senderId;
        this.content = content;
        this.messageType = messageType;
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

    @Override
    public String toString() {
        return senderId + ": " + content + "[" + messageType + "]";
    }
}
