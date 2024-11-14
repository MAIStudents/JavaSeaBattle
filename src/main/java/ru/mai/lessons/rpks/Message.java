package ru.mai.lessons.rpks;

import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;  // Для обеспечения совместимости версий
    private String sender;
    private String content;

    public Message(String sender, String content) {
        this.sender = sender;
        this.content = content;
    }

    public String getSender() {
        return sender;
    }

    public String getContent() {
        return content;
    }

    @Override
    public String toString() {
        return sender + ": " + content;
    }
}
