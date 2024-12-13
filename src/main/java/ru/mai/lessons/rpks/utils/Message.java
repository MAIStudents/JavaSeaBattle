package ru.mai.lessons.rpks.utils;

public class Message {

    private MessageType type;
    private boolean isForMe = false;
    private String content;

    public static enum MessageType {
        attack, hit, win, accept, start, heart, restart
    }

    public MessageType getType() {
        return type;
    }

    public String getContent() {
        return content;
    }

    public boolean getIsForMe() {
        return isForMe;
    }

    public void setIsForMe(boolean forMe) {
        isForMe = forMe;
    }

    public Message(MessageType type, String content) {
        this.type = type;
        this.content = content;
    }

    @Override
    public String toString() {
        return type + ";" + isForMe + ";" + content + '\n';
    }

    public static Message fromString(String str) {
        int f = str.indexOf(";");

        MessageType t = MessageType.valueOf(str.substring(0, f));

        int s = str.indexOf(";", f + 1);

        boolean forMe = str.substring(f + 1, s).equals("true");

        Message res = new Message(t, str.substring(s + 1));

        res.setIsForMe(forMe);

        return res;
    }
}
