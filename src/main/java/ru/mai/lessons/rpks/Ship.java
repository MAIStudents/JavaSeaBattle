package ru.mai.lessons.rpks;

import java.util.ArrayList;
import java.util.List;

public class Ship {
    private final int size;
    private final List<int[]> coordinates;

    public Ship(int size) {
        this.size = size;
        this.coordinates = new ArrayList<>();
    }

    public int getSize() {
        return size;
    }

    public List<int[]> getCoordinates() {
        return coordinates;
    }

    public void addCoordinate(int x, int y) {
        coordinates.add(new int[]{x, y});
    }

    public boolean isSunk(char[][] grid) {
        for (int[] coord : coordinates) {
            int x = coord[0];
            int y = coord[1];
            if (grid[x][y] != 'X') {
                return false;
            }
        }
        return true;
    }

    public boolean containsCoordinate(int x, int y) {
        for (int[] coord : coordinates) {
            int xValue = coord[0];
            int yValue = coord[1];
            if (xValue == x && yValue == y) {
                return true;
            }
        }
        return false;
    }
}
