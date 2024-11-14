package ru.mai.lessons.rpks;

import javafx.scene.control.Button;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class GameController {
    private final int MAX_SHIP = 4;
    static public class Pair<T, V>{
        T first;
        V second;
        public Pair(T first, V second) {
            this.first = first;
            this.second = second;
        }
    }

    private final List<List<Point>> battlefield = new ArrayList<>(10);
    public List<List<Button>> buttons = new ArrayList<>(10);

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
            buttons.get(x).get(y).setStyle("-fx-background-color: green;");
        } else if (canAddShip(x, y)) {
            battlefield.get(x).get(y).isTaken = true;
            buttons.get(x).get(y).setStyle("-fx-background-color: green;");
        }
    }
    public void removeShipCell(int x, int y, Button btn) {
        if (battlefield.get(x).get(y).isTaken) {
            battlefield.get(x).get(y).isTaken = false;
            btn.setStyle("");
            Pair<Integer, Integer> next = getNearShipPoint(x, y);
            while (next != null) {
                battlefield.get(next.first).get(next.second).isTaken = false;
                buttons.get(next.first).get(next.second).setStyle("");
                next = getNearShipPoint(next.first, next.second);
            }
        }

    }

    public boolean canImproveShip(int x, int y) {
        var coords = getNearShipPoint(x, y);
        if (coords == null) return false;

        int shipX = coords.first;
        int shipY = coords.second;

        battlefield.get(shipX).get(shipY).isTaken = false;
        var noMoreShips = canAddShip(x, y);
        battlefield.get(shipX).get(shipY).isTaken = true;

        if (!noMoreShips){
            System.out.printf("No more ships\n");
            return false;
        }

        int direction = getDirecton(shipX, shipY);
        System.out.printf("Direction: %d, %d, %d\n", direction, shipX, shipY);

        if (direction == -1 || direction == 1) return true;
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

        while (!stack.isEmpty()) {
            Pair<Integer, Integer> next = stack.pop();
            battlefield.get(next.first).get(next.second).isTaken = true;
        }

        if (y + 1 < 10 && battlefield.get(x).get(y+1).isTaken ||
                y - 1 > 0 && battlefield.get(x).get(y-1).isTaken){
            return -lent;
        }
        return lent;
    }
    private Pair<Integer, Integer> getNearShipPoint(int x, int y) {
        int shipX;
        int shipY;
        if (x-1 >= 0 && battlefield.get(x-1).get(y).isTaken){
            shipX = x-1;
            shipY = y;
        } else if (x + 1 < 10 && battlefield.get(x+1).get(y).isTaken){
            shipX = x+1;
            shipY = y;
        } else if (y + 1 < 10 && battlefield.get(x).get(y+1).isTaken){
            shipX = x;
            shipY = y+1;
        } else if (y - 1 >= 0 && battlefield.get(x).get(y-1).isTaken){
            shipX = x;
            shipY = y-1;
        } else{
            return null;
        }
        System.out.printf("Coords X=%d Y=%d\n", shipX, shipY);
        return new Pair<>(shipX, shipY);
    }

    public boolean canAddShip(int x, int y) {
        for (int i = max(x-1, 0); i < min(10, x+2); i++){
            for (int j = max(y-1, 0); j < min(10, y+2); j++){
                if (battlefield.get(i).get(j).isTaken){
                    return false;
                }
            }
        }
        return true;
    }

}
