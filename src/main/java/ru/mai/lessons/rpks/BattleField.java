package ru.mai.lessons.rpks;

import javafx.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class BattleField {
    private char[][] grid;
    private final List<Ship> ships;


    public char[][] getGrid() {
        return grid;
    }

    public BattleField() {
        this.grid = new char[10][10];
        this.ships = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                grid[i][j] = '.';
            }
        }
    }

    public void printField() {
        System.out.println("  0 1 2 3 4 5 6 7 8 9");
        for (int i = 0; i < 10; i++) {
            System.out.print(i + " ");
            for (int j = 0; j < 10; j++) {
                System.out.print(grid[i][j] + " ");
            }
            System.out.println();
        }
    }

    public boolean canPlaceShip(Ship ship, int x, int y, String direction) {
        if (ships.size() + 1 > 10) {
            return false;
        }
        int dx = 0, dy = 0;

        if (ship.getSize() > 1) {
            switch (direction.toLowerCase()) {
                case "right": dy = 1; break;
                case "left": dy = -1; break;
                case "down": dx = 1; break;
                case "up": dx = -1; break;
                default: return false;
            }
        }

        for (int i = 0; i < ship.getSize(); i++) {
            int nx = x + i * dx;
            int ny = y + i * dy;

            if (!isInBounds(nx, ny) || grid[nx][ny] != '.') {
                return false;
            }

            for (int a = -1; a <= 1; a++) {
                for (int b = -1; b <= 1; b++) {
                    int checkX = nx + a;
                    int checkY = ny + b;
                    if (isInBounds(checkX, checkY) && grid[checkX][checkY] == 'O') {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public void placeShip(Ship ship, int x, int y, String direction) {
        if (!canPlaceShip(ship, x, y, direction)) {
            return;
        }

        int dx = 0, dy = 0;

        if (ship.getSize() > 1) {
            switch (direction.toLowerCase()) {
                case "right": dy = 1; break;
                case "left": dy = -1; break;
                case "down": dx = 1; break;
                case "up": dx = -1; break;
            }
        }

        for (int i = 0; i < ship.getSize(); i++) {
            int nx = x + i * dx;
            int ny = y + i * dy;
            grid[nx][ny] = 'O';
            ship.addCoordinate(nx, ny);
        }
        ships.add(ship);
    }

    private boolean isInBounds(int x, int y) {
        return x >= 0 && x < 10 && y >= 0 && y < 10;
    }

    public void clearGrid() {
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                grid[i][j] = '.';
            }
        }
        ships.clear();
    }

    public void shootAtCoordinates(int x, int y) {
        if (!isInBounds(x, y)) {
            return;
        }
        if (grid[x][y] == 'O') {
            grid[x][y] = 'X';
        } else if (grid[x][y] == '.') {
            grid[x][y] = '-';
        }
    }

    public boolean areShipsAlive() {
        for (Ship ship : ships) {
            for (int[] coord : ship.getCoordinates()) {
                int x = coord[0];
                int y = coord[1];
                if (grid[x][y] == 'O') {
                    return true;
                }
            }
        }
        return false;
    }


    public List<Pair<Integer, Integer>> getYellowArea(int x, int y) {
        List<Pair<Integer, Integer>> list = new ArrayList<>();
        Ship currentShip = null;
        for (Ship ship : ships) {
            if (ship.containsCoordinate(x, y)) {
                currentShip = ship;
            }
        }

        if (currentShip == null) {
            return null;
        }


        for (int[] coord : currentShip.getCoordinates()) {
            int xCoord = coord[0];
            int yCoord = coord[1];

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    int nx = xCoord + dx;
                    int ny = yCoord + dy;
                    if (isInBounds(nx, ny) && grid[nx][ny] == '.') {
                        list.add(new Pair<>(nx, ny));
                    }
                }
            }
        }
        return list;
    }

    public boolean isHit(int x, int y) {
        return grid[x][y] == 'X';
    }

    public boolean isShipSunk(int x, int y) {
        for (Ship ship : ships) {
            if (ship.containsCoordinate(x, y)) {
                return ship.isSunk(grid);
            }
        }
        return false;
    }


}
