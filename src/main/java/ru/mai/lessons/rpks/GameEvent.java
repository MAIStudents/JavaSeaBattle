package ru.mai.lessons.rpks;

import java.util.ArrayList;
import java.util.List;

class GameEvent{
    public enum State{
        MISSED,
        HURT
    }
    public State state;
    public  int x;
    public  int y;
    public GameEvent(State state, int x, int y){
        this.state = state;
        this.x = x;
        this.y = y;
    }
    public List<GameEvent> getEvents(String s){
        List<GameEvent> events = new ArrayList<>();
        String[] eventStrings = s.split(";");

        for (String eventStr : eventStrings) {
            if (eventStr.isEmpty()) continue;

            String[] parts = eventStr.split(",");
            if (parts.length != 3) continue;

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
    @Override
    public String toString() {
        return state.toString() + "," + x + "," + y + ";";
    }
}