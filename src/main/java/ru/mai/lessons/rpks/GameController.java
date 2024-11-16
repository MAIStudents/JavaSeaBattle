package ru.mai.lessons.rpks;

import javafx.scene.control.Button;

import java.util.*;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class GameController {
    private final String missedStyle = "-fx-background-color: gray;";
    private final String hurtStyle = "-fx-background-color: red;";
    private final int MAX_SHIP = 4;
    static public class Pair<T, V> {
        T first;
        V second;
        public Pair(T first, V second) {
            this.first = first;
            this.second = second;
        }
    }
    private final HashMap<Integer, Integer> ships = new HashMap<>();

    private final List<List<Point>> battlefield = new ArrayList<>(10);
    public final List<List<Button>> buttons = new ArrayList<>(10);
    public final List<List<Button>> enemyButtons = new ArrayList<>(10);

    public void prepareMove() {
        for (var lst : enemyButtons) {
            for (var btn : lst) {
                btn.setDisable(false);
            }
        }
    }

    public void endMove() {
        for (var lst : enemyButtons) {
            for (var btn : lst) {
                btn.setDisable(true);
            }
        }
    }
    public void colorPoints(List<GameEvent> points, List<List<Button>> buttons) {
        for (var point : points) {
            if (point.state == GameEvent.State.MISSED) {
                buttons.get(point.x).get(point.y).setStyle(missedStyle);
            } else {
                buttons.get(point.x).get(point.y).setStyle(hurtStyle);
            }
        }
    }

    public Pair<List<GameEvent>, Boolean> enemyMakeStep(List<GameEvent> points) {
        // дают ход противника, просчитываем изменения на нашем поле
        List<GameEvent> result = new ArrayList<>();
        if (points.size() != 1) {
            return null;
        }
        GameEvent event = points.get(0);
        if (!battlefield.get(event.x).get(event.y).isTaken) {
            result.add(new GameEvent(GameEvent.State.MISSED, event.x, event.y));
        } else {
            List<Pair<Integer, Integer>> ship = getFullShip(event.x, event.y);
            battlefield.get(event.x).get(event.y).isAlive = false;
            result.add(new GameEvent(GameEvent.State.HURT, event.x, event.y));
            boolean destroyed = true;
            for (var cords : ship) {
                if (battlefield.get(cords.first).get(cords.second).isAlive) {
                    destroyed = false;
                }
            }
            if (destroyed) {
                for (var cords : ship) {
                    var ptr = getAreaAroundShip(cords.first, cords.second);
                    for (var point : ptr) {
                        result.add(new GameEvent(GameEvent.State.MISSED, point.first, point.second));
                    }
                }
            }
        }
        colorPoints(result, buttons);
        return new Pair<>(result, isLost());
    }
    private boolean isLost() {
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                if (battlefield.get(i).get(j).isAlive) {
                    return false;
                }
            }
        }
        return true;
    }
    private List<Pair<Integer, Integer>> getAreaAroundShip(int x, int y) {
        List<Pair<Integer, Integer>> result = new ArrayList<>();
        for (int i = max(x - 1, 0); i < min(x + 2, 10); i++) {
            for (int j = max(y - 1, 0); j < min(y + 2, 10); j++) {
                if (!battlefield.get(i).get(j).isTaken) {
                    result.add(new Pair<>(i, j));
                }
            }
        }
        return result;
    }
    private List<Pair<Integer, Integer>>getFullShip(int x, int y) {
        List<Pair<Pair<Integer, Integer>, Integer>> result = new ArrayList<>();
        Pair<Integer, Integer> next = new Pair<>(x, y);

        getShipPart(result, next);
        next = getNearShipPoint(x, y);
        getShipPart(result, next);
        List<Pair<Integer, Integer>> points = new ArrayList<>();
        for (var p : result) {
            points.add(p.first);
            battlefield.get(p.first.first).get(p.first.second).isTaken = p.second == 1;
        }
        return points;

    }

    private void getShipPart(List<Pair<Pair<Integer, Integer>, Integer>> result, Pair<Integer, Integer> next) {
        while (next != null) {
            result.add(new Pair<>(next, battlefield.get(next.first).get(next.second).isTaken ? 1 : 0));
            battlefield.get(next.first).get(next.second).isTaken = false;
            next = getNearShipPoint(next.first, next.second);
        }
    }

    public boolean checkField() {
        // if true then
        for (var list : buttons) {
            for (var btn : list) {
                btn.setDisable(true);
            }
        }
        // endif
        return true;  // debug


        /*
        for (int i = 1, cnt = 4; i <= MAX_SHIP; i++, cnt--) {
            if (!ships.containsKey(i) || ships.get(i) != cnt) {
                return false;
            }
        }
        return true; */
    }

    public void clearBattlefield() {
        battlefield.clear();
        for (int i = 0; i < 10; i++) {
            List<Point> row = new ArrayList<>(10);

            for (int j = 0; j < 10; j++) {
                row.add(new Point());
                buttons.get(i).get(j).setStyle("");
            }
            battlefield.add(row);
        }
    }
    public void addShipCell(int x, int y, Button btn) {
        if (canImproveShip(x, y)) {
            battlefield.get(x).get(y).isTaken = true;
            battlefield.get(x).get(y).isAlive = true;
            buttons.get(x).get(y).setStyle("-fx-background-color: green;");
            int size = Math.abs(getDirecton(x, y));
            ships.put(size - 1, ships.get(size - 1) - 1);
            if (ships.containsKey(size)) {
                ships.put(size, ships.get(size) + 1);
            } else {
                ships.put(size, 1);
            }
        } else if (canAddShip(x, y)) {
            battlefield.get(x).get(y).isTaken = true;
            battlefield.get(x).get(y).isAlive = true;
            buttons.get(x).get(y).setStyle("-fx-background-color: green;");
            if (ships.containsKey(1)) {
                ships.put(1, ships.get(1) + 1);
            } else {
                ships.put(1, 1);
            }
        }
    }
    public void removeShipCell(int x, int y, Button btn) {
        if (battlefield.get(x).get(y).isTaken) {
            int size = Math.abs(getDirecton(x, y));
            ships.put(size, ships.get(size) - 1);

            battlefield.get(x).get(y).isTaken = false;
            btn.setStyle("");
            Pair<Integer, Integer> next = getNearShipPoint(x, y);
            while (next != null) {
                battlefield.get(next.first).get(next.second).isTaken = false;
                buttons.get(next.first).get(next.second).setStyle("");
                next = getNearShipPoint(next.first, next.second);
            }
            next = getNearShipPoint(x, y);
            while (next != null) {
                battlefield.get(next.first).get(next.second).isTaken = false;
                buttons.get(next.first).get(next.second).setStyle("");
                next = getNearShipPoint(next.first, next.second);
            }
        }

    }

    public boolean canImproveShip(int x, int y) {
        var coords = getNearShipPoint(x, y);
        if (coords == null)  {
            return false;
        }

        int shipX = coords.first;
        int shipY = coords.second;

        battlefield.get(shipX).get(shipY).isTaken = false;
        var noMoreShips = canAddShip(x, y);
        battlefield.get(shipX).get(shipY).isTaken = true;

        if (!noMoreShips) {
            System.out.printf("No more ships\n");
            return false;
        }

        int direction = getDirecton(shipX, shipY);
        System.out.printf("Direction: %d, %d, %d\n", direction, shipX, shipY);

        if (direction == -1 || direction == 1)  {
            return true ;
        }
        if (Math.abs(direction) + 1 > MAX_SHIP) {
            return false;
        }

        if (direction > 0) {
            return shipY == y;
        }
        return shipX == x;

    }
    private int getDirecton(int x, int y) {
        Deque<Pair<Integer, Integer>> stack = new LinkedList<>();
        stack.push(new Pair<>(x, y));
        battlefield.get(x).get(y).isTaken = false;

        int lent = 1;
        var cords = getNearShipPoint(x, y);
        while (cords != null) {
            lent ++;
            stack.push(cords);
            battlefield.get(cords.first).get(cords.second).isTaken = false;
            cords = getNearShipPoint(cords.first, cords.second);
        }

        cords = getNearShipPoint(x, y);
        while (cords != null) {
            lent ++;
            stack.push(cords);
            battlefield.get(cords.first).get(cords.second).isTaken = false;
            cords = getNearShipPoint(cords.first, cords.second);
        }

        while (!stack.isEmpty()) {
            Pair<Integer, Integer> next = stack.pop();
            battlefield.get(next.first).get(next.second).isTaken = true;
        }

        if (y + 1 < 10 && battlefield.get(x).get(y + 1).isTaken ||
                y - 1 > 0 && battlefield.get(x).get(y - 1).isTaken) {
            return -lent;
        }
        return lent;
    }
    private Pair<Integer, Integer> getNearShipPoint(int x, int y) {
        int shipX;
        int shipY;
        if (x - 1 >= 0 && battlefield.get(x - 1).get(y).isTaken) {
            shipX = x - 1;
            shipY = y;
        } else if (x + 1 < 10 && battlefield.get(x + 1).get(y).isTaken) {
            shipX = x + 1;
            shipY = y;
        } else if (y + 1 < 10 && battlefield.get(x).get(y + 1).isTaken) {
            shipX = x;
            shipY = y + 1;
        } else if (y - 1 >= 0 && battlefield.get(x).get(y - 1).isTaken) {
            shipX = x;
            shipY = y - 1;
        } else {
            return null;
        }
        System.out.printf("Coords X=%d Y=%d\n", shipX, shipY);
        return new Pair<>(shipX, shipY);
    }

    public boolean canAddShip(int x, int y) {
        for (int i = max(x - 1, 0); i < min(10, x + 2); i++) {
            for (int j = max(y - 1, 0); j < min(10, y + 2); j++) {
                if (battlefield.get(i).get(j).isTaken) {
                    return false;
                }
            }
        }
        return true;
    }
}
