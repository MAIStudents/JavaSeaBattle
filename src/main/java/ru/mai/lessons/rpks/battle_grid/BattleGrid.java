package ru.mai.lessons.rpks.battle_grid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class BattleGrid {
    public static enum BATTLE_GRID_STATE {
        OCCUPIED,
        NOT_OCCUPIED,
        HIT
    }

    public static final int GRID_SIZE = 10;
    private BATTLE_GRID_STATE[][] battleGrid;
    private static final int[] SHIP_SIZES = {4, 3, 3, 2, 2, 2, 1, 1, 1, 1};

    public BattleGrid() {
        battleGrid = new BATTLE_GRID_STATE[GRID_SIZE][GRID_SIZE];

        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                battleGrid[row][col] = BATTLE_GRID_STATE.NOT_OCCUPIED;
            }
        }
    }

    public void setCell(int row, int col, BATTLE_GRID_STATE state) {
        battleGrid[row][col] = state;
    }

    public boolean isOccupied(int row, int col) {
        return battleGrid[row][col].equals(BATTLE_GRID_STATE.OCCUPIED);
    }

    public boolean isNotOccupied(int row, int col) {
        return battleGrid[row][col].equals(BATTLE_GRID_STATE.NOT_OCCUPIED);
    }

    public void fillBattleGridRandomly() {
        clearBattleGrid();
        List<Integer> shipSizes = new ArrayList<>();

        for (int size : SHIP_SIZES) {
            shipSizes.add(size);
        }

        Collections.shuffle(shipSizes);
        placeShipsRandomly(shipSizes);
    }

    private void clearBattleGrid() {
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                battleGrid[row][col] = BATTLE_GRID_STATE.NOT_OCCUPIED;
            }
        }
    }

    private void placeShipsRandomly(List<Integer> shipSizes) {
        Random random = new Random();

        for (Integer shipSize : shipSizes) {
            boolean horizontal = random.nextBoolean();
            int row = random.nextInt(GRID_SIZE);
            int col = random.nextInt(GRID_SIZE);

            while (!isCellAvailable(row, col, shipSize, horizontal)) {
                row = random.nextInt(GRID_SIZE);
                col = random.nextInt(GRID_SIZE);
            }

            if (horizontal) {
                for (int i = 0; i < shipSize; i++) {
                    battleGrid[row][col + i] = BattleGrid.BATTLE_GRID_STATE.OCCUPIED;
                }
            } else {
                for (int i = 0; i < shipSize; i++) {
                    battleGrid[row + i][col] = BATTLE_GRID_STATE.OCCUPIED;
                }
            }
        }
    }

    private boolean isCellAvailable(int startRow, int startCol, int shipSize, boolean horizontal) {
        if (horizontal) {
            if (startCol + shipSize >= GRID_SIZE) {
                return false;
            }

            for (int i = startCol; i < startCol + shipSize; i++) {
                if (isOccupied(i, startCol) || isAdjacentCellEmpty(startRow, i)) {
                    return false;
                }
            }
        } else {
            if (startRow + shipSize >= GRID_SIZE) {
                return false;
            }

            for (int i = startRow; i < startRow + shipSize; i++) {
                if (isOccupied(i, startCol) || isAdjacentCellEmpty(i, startCol)) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean isAdjacentCellEmpty(int row, int col) {
        int checkStartRow = Math.max(0, row - 1);
        int checkStartCol = Math.max(0, col - 1);
        int checkEndRow = Math.min(GRID_SIZE - 1, row + 1);
        int checkEndCol = Math.min(GRID_SIZE - 1, col + 1);

        for (int i = checkStartRow; i <= checkEndRow; i++) {
            for (int j = checkStartCol; j <= checkEndCol; j++) {
                if (isOccupied(i, j)) {
                    return true;
                }
            }
        }

        return false;
    }
}
