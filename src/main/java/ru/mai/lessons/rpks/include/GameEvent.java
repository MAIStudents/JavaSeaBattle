package ru.mai.lessons.rpks.include;

import java.util.ArrayList;
import java.util.List;

public class GameEvent {
    private final State state;
    private final int x;
    private final int y;

    public GameEvent(State state, int x, int y) {
        this.state = state;
        this.x = x;
        this.y = y;
    }

    public static List<GameEvent> parseEvents(String input) {
        List<GameEvent> events = new ArrayList<>();
        if (input == null || input.isBlank()) {
            return events;
        }

        String[] eventStrings = input.split(";");
        for (String eventStr : eventStrings) {
            if (eventStr.isBlank()) {
                continue;
            }
            try {
                String[] parts = eventStr.split(",");
                if (parts.length != 3) {
                    throw new IllegalArgumentException("Invalid event format: " + eventStr);
                }

                int stateValue = Integer.parseInt(parts[0].trim());
                int x = Integer.parseInt(parts[1].trim());
                int y = Integer.parseInt(parts[2].trim());

                State state = parseState(stateValue);
                events.add(new GameEvent(state, x, y));
            } catch (IllegalArgumentException e) {
                System.err.println("Error parsing event: " + eventStr + " - " + e.getMessage());
            }
        }
        return events;
    }

    private static State parseState(int stateValue) {
        return switch (stateValue) {
            case 0 -> State.MISSED;
            case 1 -> State.HURT;
            default -> throw new IllegalArgumentException("Unknown state value: " + stateValue);
        };
    }

    public static String eventsToString(List<GameEvent> events) {
        StringBuilder result = new StringBuilder();
        for (GameEvent event : events) {
            result.append(event.toString());
        }
        return result.toString();
    }

    public static boolean containsOnlyMissed(List<GameEvent> events) {
        return events.size() == 1 && events.get(0).state == State.MISSED;
    }

    private static String stateToString(State state) {
        return switch (state) {
            case MISSED -> "0";
            case HURT -> "1";
        };
    }

    @Override
    public String toString() {
        return stateToString(state) + "," + x + "," + y + ";";
    }

    public State getState() {
        return state;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public enum State {
        MISSED,
        HURT
    }
}
