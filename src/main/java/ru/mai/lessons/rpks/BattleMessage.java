package ru.mai.lessons.rpks;

import java.util.List;

public class BattleMessage {
    public enum MessageType {
        HEARTBEAT,
        STEP,
        RESPONSE,
        TURN,
        DISCONNECT,
        GAME_OVER,
        START,
    }
    public String messageLine;
    public List<GameEvent> gameEvents;
    public MessageType messageType;

    public static BattleMessage getMessageFromString(String message) {
        BattleMessage msg = new BattleMessage();
        String[] lst = message.split("&");

        msg.messageType = getMessageTypeFromString(lst[0]);
        if (msg.messageType == MessageType.RESPONSE ||
                msg.messageType == MessageType.STEP ||
                msg.messageType == MessageType.GAME_OVER) {
            msg.messageLine = lst[1];
            msg.gameEvents = getGameEventsFromString(lst[1]);
        } else if (lst.length > 1) {
            msg.messageLine = lst[1];
        }
        return msg;
    }
    @Override
    public String toString() {
        return messageType.ordinal() + "&" + messageLine;

    }

    public static MessageType getMessageTypeFromString(String type) {
        return switch (type) {
            case "0" -> MessageType.HEARTBEAT;
            case "1" -> MessageType.STEP;
            case "2" -> MessageType.RESPONSE;
            case "3" -> MessageType.TURN;
            case "4" -> MessageType.DISCONNECT;
            case "5" -> MessageType.GAME_OVER;
            case "6" -> MessageType.START;
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };
    }

    public static List<GameEvent> getGameEventsFromString(String gameEvents) {
        return  GameEvent.getEvents(gameEvents);
    }
}
