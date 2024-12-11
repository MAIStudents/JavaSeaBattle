package ru.mai.lessons.rpks.include;

import java.util.ArrayList;
import java.util.List;

public record GameEvent(ru.mai.lessons.rpks.include.GameEvent.State state, int x, int y) {

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
            case 0 -> State.MISS;
            case 1 -> State.HIT;
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
        return events.size() == 1 && events.get(0).state == State.MISS;
    }

    private static String stateToString(State state) {
        return switch (state) {
            case MISS -> "0";
            case HIT -> "1";
        };
    }

    @Override
    public String toString() {
        return stateToString(state) + "," + x + "," + y + ";";
    }

    public enum State {
        MISS,
        HIT
    }
}
