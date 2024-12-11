package ru.mai.lessons.rpks.controllers;

import ru.mai.lessons.rpks.include.GameEvent;

import java.util.*;

public class MessageController {

    private final String messageContent;
    private final List<GameEvent> gameEvents;
    private final MessageType messageType;

    private static final Map<String, MessageType> MESSAGE_TYPE_MAP = Map.of(
            "0", MessageType.HEARTBEAT,
            "1", MessageType.STEP,
            "2", MessageType.RESPONSE,
            "3", MessageType.TURN,
            "4", MessageType.DISCONNECT,
            "5", MessageType.GAME_OVER,
            "6", MessageType.START
    );

    /**
     * @param rawMessage message str
     * @return obj BattleMessage
     */
    public static MessageController parseFromRawMessage(String rawMessage) {
        validateInputMessage(rawMessage);

        String[] parts = rawMessage.split("#");
        validateMessageFormat(parts);

        MessageType type = extractMessageType(parts[0]);
        String content = extractMessageContent(parts);

        List<GameEvent> events = extractGameEventsIfNeeded(type, content);

        return new MessageController(type, content, events);
    }

    private static void validateInputMessage(String rawMessage) {
        if (rawMessage == null || rawMessage.isEmpty()) {
            throw new IllegalArgumentException("Input message cannot be null or empty.");
        }
    }

    private static void validateMessageFormat(String[] parts) {
        if (parts.length < 1) {
            throw new IllegalArgumentException("Invalid message format: insufficient parts.");
        }
    }

    private static MessageType extractMessageType(String typeId) {
        if (!MESSAGE_TYPE_MAP.containsKey(typeId)) {
            throw new IllegalStateException("Unexpected message type: " + typeId);
        }
        return MESSAGE_TYPE_MAP.get(typeId);
    }

    private static String extractMessageContent(String[] parts) {
        return parts.length > 1 ? parts[1] : "";
    }

    private static List<GameEvent> extractGameEventsIfNeeded(MessageType type, String content) {
        if (type == MessageType.RESPONSE || type == MessageType.STEP || type == MessageType.GAME_OVER) {
            return parseGameEvents(content);
        }
        return Collections.emptyList();
    }

    private static List<GameEvent> parseGameEvents(String content) {
        if (content == null || content.isEmpty()) {
            return Collections.emptyList();
        }
        return GameEvent.parseEvents(content);
    }

    private MessageController(MessageType type, String content, List<GameEvent> events) {
        this.messageType = type;
        this.messageContent = content;
        this.gameEvents = events;
    }

    @Override
    public String toString() {
        return messageType.ordinal() + "#" + (messageContent != null ? messageContent : "");
    }

    public String getMessageContent() {
        return messageContent;
    }

    public List<GameEvent> getGameEvents() {
        return gameEvents;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public enum MessageType {
        HEARTBEAT,
        STEP,
        RESPONSE,
        TURN,
        DISCONNECT,
        GAME_OVER,
        START
    }
}
