package ru.mai.lessons.rpks.controllers;

import javafx.scene.control.Button;
import javazoom.jl.player.Player;
import ru.mai.lessons.rpks.include.GameEvent;
import ru.mai.lessons.rpks.include.Pair;
import ru.mai.lessons.rpks.include.Point;


import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.*;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class GameController {
    private final String STYLE_SEA = "-fx-background-image: url('" + getClass().getResource("/images/sea.jpg").toExternalForm() + "');"
            + "-fx-background-size: cover;"
            + "-fx-border-color: black;"
            + "-fx-border-width: 1px;";

    private final String SHIP_STYLE = "-fx-background-image: url('" + getClass().getResource("/images/shipp.png").toExternalForm() + "');"
            + "-fx-background-size: cover;"
            + "-fx-border-color: black;"
            + "-fx-border-width: 1px;";

    private final String MISS_STYLE = "-fx-background-image: url('" + getClass().getResource("/images/miss.png").toExternalForm() + "');"
            + "-fx-background-size: cover;"
            + "-fx-border-color: black;"
            + "-fx-border-width: 1px;";

    private final String FIRE_STYLE = "-fx-background-image: url('" + getClass().getResource("/images/fire.gif").toExternalForm() + "');" +
            "-fx-background-size: cover;" +
            "-fx-border-color: black;" +
            "-fx-border-width: 1px;";

    private final int MAX_SHIP = 4;

    private final HashMap<Integer, Integer> ships = new HashMap<>();

    private final List<List<Point>> battlefield = new ArrayList<>(10);
    public final List<List<Button>> buttons = new ArrayList<>(10);
    public final List<List<Button>> enemyButtons = new ArrayList<>(10);

    public void clearFields() {
        ships.clear();
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                enemyButtons.get(i).get(j).setStyle(STYLE_SEA);
                enemyButtons.get(i).get(j).setDisable(true);
                buttons.get(j).get(i).setDisable(false);
                buttons.get(j).get(i).setStyle(STYLE_SEA);
                battlefield.get(i).get(j).isTaken = false;
                battlefield.get(i).get(j).isAlive = false;
            }
        }
    }

    public boolean isCellCanBeAttacked(int x, int y) {
        String str = enemyButtons.get(x).get(y).getStyle();
        return Objects.equals(str, STYLE_SEA);
    }

    public void colorPoints(List<GameEvent> points, List<List<Button>> buttons) {
        for (GameEvent point : points) {
            if (point.state() == GameEvent.State.MISS) {
                buttons.get(point.x()).get(point.y()).setStyle(MISS_STYLE);
            } else {
                buttons.get(point.x()).get(point.y()).setStyle(FIRE_STYLE);
            }
        }
    }

    public void playSound(String soundFilePath) {
        new Thread(() -> {
            try (InputStream audioSrc = getClass().getResourceAsStream(soundFilePath);
                 InputStream bufferedIn = new BufferedInputStream(audioSrc)) {
                Player player = new Player(bufferedIn);
                player.play();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }



    public Pair<List<GameEvent>, Boolean> enemyMakeStep(List<GameEvent> points) {
        List<GameEvent> result = new ArrayList<>();
        if (points.size() != 1) {
            return null;
        }
        GameEvent event = points.get(0);
        if (!battlefield.get(event.x()).get(event.y()).isTaken) {
            playSound("/sounds/miss.mp3");
            result.add(new GameEvent(GameEvent.State.MISS, event.x(), event.y()));
        } else {
            playSound("/sounds/hitt.mp3");
            List<Pair<Integer, Integer>> ship = getFullShip(event.x(), event.y());
            battlefield.get(event.x()).get(event.y()).isAlive = false;
            result.add(new GameEvent(GameEvent.State.HIT, event.x(), event.y()));
            boolean destroyed = true;
            for (var cords : ship) {
              if (battlefield.get(cords.first()).get(cords.second()).isAlive) {
                destroyed = false;
                break;
              }
            }
            if (destroyed) {
                for (var cords : ship) {
                    var ptr = getAreaAroundShip(cords.first(), cords.second());
                    for (var point : ptr) {
                        result.add(new GameEvent(GameEvent.State.MISS, point.first(), point.second()));
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
            points.add(p.first());
            battlefield.get(p.first().first()).get(p.first().second()).isTaken = p.second() == 1;
        }
        return points;

    }

    private void getShipPart(List<Pair<Pair<Integer, Integer>, Integer>> result, Pair<Integer, Integer> next) {
        while (next != null) {
            result.add(new Pair<>(next, battlefield.get(next.first()).get(next.second()).isTaken ? 1 : 0));
            battlefield.get(next.first()).get(next.second()).isTaken = false;
            next = getNearShipPoint(next.first(), next.second());
        }
    }

    public boolean checkField() {
        for (int i = 1, cnt = 4; i <= MAX_SHIP; i++, cnt--) {
            if (!ships.containsKey(i) || ships.get(i) != cnt) {
                return false;
            }
        }
         for (var list : buttons) {
            for (var btn : list) {
                btn.setDisable(true);
            }
        }
        return true;
    }

    public void clearBattlefield() {
        battlefield.clear();
        for (int i = 0; i < 10; i++) {
            List<Point> row = new ArrayList<>(10);
            for (int j = 0; j < 10; j++) {
                row.add(new Point());
                buttons.get(i).get(j).setStyle("");
                buttons.get(i).get(j).setMinSize(40, 40);
            }
            battlefield.add(row);
        }
    }

    public void addShipCell(int x, int y) {
        if (canImproveShip(x, y)) {
            battlefield.get(x).get(y).isTaken = true;
            battlefield.get(x).get(y).isAlive = true;
            buttons.get(x).get(y).setStyle(SHIP_STYLE);
            int size = Math.abs(getShipDirection(x, y));
            ships.put(size - 1, ships.get(size - 1) - 1);
            if (ships.containsKey(size)) {
                ships.put(size, ships.get(size) + 1);
            } else {
                ships.put(size, 1);
            }
        } else if (canAddShip(x, y)) {
            battlefield.get(x).get(y).isTaken = true;
            battlefield.get(x).get(y).isAlive = true;
            buttons.get(x).get(y).setStyle(SHIP_STYLE);
            if (ships.containsKey(1)) {
                ships.put(1, ships.get(1) + 1);
            } else {
                ships.put(1, 1);
            }
        }
    }

    public void removeShipCell(int x, int y, Button btn) {
        if (battlefield.get(x).get(y).isTaken) {
            handleShipSizeUpdate(x, y);
            updateCellState(x, y, btn);
            clearAdjacentCells(x, y);
        }
    }

    private void handleShipSizeUpdate(int x, int y) {
        int size = Math.abs(getShipDirection(x, y));
        ships.put(size, ships.get(size) - 1);
    }

    private void updateCellState(int x, int y, Button btn) {
        battlefield.get(x).get(y).isTaken = false;
        battlefield.get(x).get(y).isAlive = false;

        btn.setStyle(STYLE_SEA);
    }

    private void clearAdjacentCells(int x, int y) {
        Pair<Integer, Integer> next = getNearShipPoint(x, y);
        while (next != null) {
            battlefield.get(next.first()).get(next.second()).isTaken = false;
            battlefield.get(next.first()).get(next.second()).isAlive = false;
            buttons.get(next.first()).get(next.second()).setStyle(STYLE_SEA);
            Pair<Integer, Integer> adjacent = getNearShipPoint(next.first(), next.second());
            if (adjacent != null) {
                next = adjacent;
            } else {
                next = getNearShipPoint(x, y);
            }
        }
    }


    public boolean canImproveShip(int x, int y) {
        Pair<Integer, Integer> adjacentShipPoint = getNearShipPoint(x, y);
        if (adjacentShipPoint == null) {
            return false;
        }

        int shipX = adjacentShipPoint.first();
        int shipY = adjacentShipPoint.second();

        battlefield.get(shipX).get(shipY).isTaken = false;

        boolean canAddNewShip = canAddShip(x, y);

        battlefield.get(shipX).get(shipY).isTaken = true;

        if (!canAddNewShip) {
            return false;
        }
        int shipDirection = getShipDirection(shipX, shipY);

        if (Math.abs(shipDirection) == 1) {
            return true;
        }

        if (Math.abs(shipDirection) + 1 > MAX_SHIP) {
            return false;
        }

        return (shipDirection > 0 && shipY == y) || (shipDirection < 0 && shipX == x);
    }


    private int getShipDirection(int x, int y) {
        Deque<Pair<Integer, Integer>> stack = new LinkedList<>();
        stack.push(new Pair<>(x, y));
        battlefield.get(x).get(y).isTaken = false;

        int shipLength = 1;
        shipLength += traverseAndReset(x, y, stack, true);
        shipLength += traverseAndReset(x, y, stack, false);

        while (!stack.isEmpty()) {
            Pair<Integer, Integer> cell = stack.pop();
            battlefield.get(cell.first()).get(cell.second()).isTaken = true;
        }

        if ((y + 1 < 10 && battlefield.get(x).get(y + 1).isTaken) ||
                (y - 1 >= 0 && battlefield.get(x).get(y - 1).isTaken)) {
            return -shipLength;
        }
        return shipLength;
    }

    private int traverseAndReset(int x, int y, Deque<Pair<Integer, Integer>> stack, boolean forward) {
        int length = 0;
        Pair<Integer, Integer> adjacent = forward ? findAdjacentShipCell(x, y, true) : findAdjacentShipCell(x, y, false);

        while (adjacent != null) {
            length++;
            stack.push(adjacent);
            battlefield.get(adjacent.first()).get(adjacent.second()).isTaken = false;
            adjacent = forward ? findAdjacentShipCell(adjacent.first(), adjacent.second(), true) : findAdjacentShipCell(adjacent.first(), adjacent.second(), false);
        }

        return length;
    }

    private Pair<Integer, Integer> findAdjacentShipCell(int x, int y, boolean forward) {
        int[][] offsets = forward ? new int[][]{{1, 0}, {0, 1}} : new int[][]{{-1, 0}, {0, -1}};

        for (int[] offset : offsets) {
            int nx = x + offset[0];
            int ny = y + offset[1];
            if (nx >= 0 && nx < 10 && ny >= 0 && ny < 10 && battlefield.get(nx).get(ny).isTaken) {
                return new Pair<>(nx, ny);
            }
        }
        return null;
    }

    private Pair<Integer, Integer> getNearShipPoint(int x, int y) {
        return findAdjacentShipCell(x, y, true) != null ? findAdjacentShipCell(x, y, true) : findAdjacentShipCell(x, y, false);
    }

    public void prepareMove() {
        for (var list : enemyButtons) {
            for (var btn : list) {
                btn.setDisable(false);
            }
        }
    }

    public void endMove() {
        for (var list : enemyButtons) {
            for (var btn : list) {
                btn.setDisable(true);
            }
        }
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
