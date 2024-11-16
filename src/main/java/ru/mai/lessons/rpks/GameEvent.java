package ru.mai.lessons.rpks;

import java.util.ArrayList;
import java.util.List;

public class GameEvent {
    public enum State {
        MISSED,
        HURT
    }
    public State state;
    public  int x;
    public  int y;
    public GameEvent(State state, int x, int y) {
        this.state = state;
        this.x = x;
        this.y = y;
    }
    public static List<GameEvent> getEvents(String s) {
        List<GameEvent> events = new ArrayList<>();
        String[] eventStrings = s.split(";");

        for (String eventStr : eventStrings) {
            if (eventStr.isEmpty()) {
                continue;
            }
            String[] parts = eventStr.split(",");
            if (parts.length != 3) {
                continue;
            }

            try {
                int stateInt = Integer.parseInt(parts[0].trim());
                int x = Integer.parseInt(parts[1].trim());
                int y = Integer.parseInt(parts[2].trim());

                State state = switch (stateInt) {
                    case 0 -> State.MISSED;
                    case 1 -> State.HURT;
                    default -> throw new IllegalArgumentException("Invalid state value: " + stateInt);
                };

                events.add(new GameEvent(state, x, y));
            } catch (IllegalArgumentException e) {
                System.out.println("Error parsing event: " + eventStr + " - " + e.getMessage());
            }
        }
        return events;
    }
    public String stateToString(State state) {
        return switch (state) {
            case MISSED -> "0";
            case HURT -> "1";
        };
    }
    @Override
    public String toString() {
        return  stateToString(state) + "," + x + "," + y + ";";
    }

    public static String listToString(List<GameEvent> events) {
        StringBuilder stringBuilder = new StringBuilder();
        for (GameEvent event : events) {
            stringBuilder.append(event.toString());
        }
        return stringBuilder.toString();
    }

    public static boolean isMissed(List<GameEvent> events) {
       return (events.size() == 1 && events.get(0).state == State.MISSED);
    }
}