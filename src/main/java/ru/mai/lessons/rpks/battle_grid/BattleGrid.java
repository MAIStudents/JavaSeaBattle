package ru.mai.lessons.rpks.battle_grid;

import org.javatuples.Triplet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static ru.mai.lessons.rpks.battle_grid.BattleGridPaneUtils.GRID_SIZE;

public class BattleGrid implements Cloneable {
    public enum BATTLE_GRID_STATE {
        NOT_OCCUPIED,
        OCCUPIED,
        HIT
    }

    private static final int MAX_ITER = 100;
    private final BATTLE_GRID_STATE[][] battleGrid;
    private static final int[] SHIP_SIZES = {4, 3, 3, 2, 2, 2, 1, 1, 1, 1};

    public BattleGrid() {
        battleGrid = new BATTLE_GRID_STATE[GRID_SIZE][GRID_SIZE];

        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                battleGrid[row][col] = BATTLE_GRID_STATE.NOT_OCCUPIED;
            }
        }
    }

    public boolean isOccupied(int row, int col) {
        return battleGrid[row][col].equals(BATTLE_GRID_STATE.OCCUPIED);
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

    public void clearBattleGrid() {
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                battleGrid[row][col] = BATTLE_GRID_STATE.NOT_OCCUPIED;
            }
        }
    }

    public boolean placeShip(int row, int col, int shipSize, boolean horizontal) {
        if (isCellAvailable(row, col, shipSize, horizontal)) {
            if (horizontal) {
                for (int i = 0; i < shipSize; i++) {
                    battleGrid[row][col + i] = BattleGrid.BATTLE_GRID_STATE.OCCUPIED;
                }
            } else {
                for (int i = 0; i < shipSize; i++) {
                    battleGrid[row + i][col] = BATTLE_GRID_STATE.OCCUPIED;
                }
            }

            return true;
        }

        return false;
    }

    private void placeShipsRandomly(List<Integer> shipSizes) {
        Random random = new Random();

        for (Integer shipSize : shipSizes) {
            boolean horizontal = random.nextBoolean();
            int row = random.nextInt(GRID_SIZE);
            int col = random.nextInt(GRID_SIZE);

            int counter = 0;

            while (!isCellAvailable(row, col, shipSize, horizontal) && counter < MAX_ITER) {
                counter++;
                row = random.nextInt(GRID_SIZE);
                col = random.nextInt(GRID_SIZE);
            }

            if (counter == MAX_ITER) {
                Triplet<Integer, Integer, Boolean> cell = getCellAvailable(shipSize, horizontal);

                if (cell != null) {
                    row = cell.getValue0();
                    col = cell.getValue1();
                    horizontal = cell.getValue2();
                }
            }

            placeShip(row, col, shipSize, horizontal);
        }
    }

    private Triplet<Integer, Integer, Boolean> getCellAvailable(int shipSize, boolean horizontal) {
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                if (isCellAvailable(row, col, shipSize, horizontal)) {
                    return new Triplet<>(row, col, horizontal);
                }
            }
        }

        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                if (isCellAvailable(row, col, shipSize, !horizontal)) {
                    return new Triplet<>(row, col, !horizontal);
                }
            }
        }

        return null;
    }

    private boolean isCellAvailable(int startRow, int startCol, int shipSize, boolean horizontal) {
        if (horizontal) {
            if (startCol + shipSize >= GRID_SIZE) {
                return false;
            }

            for (int i = startCol; i < startCol + shipSize; i++) {
                if (isOccupied(startRow, i) || !isAdjacentCellEmpty(startRow, i)) {
                    return false;
                }
            }
        } else {
            if (startRow + shipSize >= GRID_SIZE) {
                return false;
            }

            for (int i = startRow; i < startRow + shipSize; i++) {
                if (isOccupied(i, startCol) || !isAdjacentCellEmpty(i, startCol)) {
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
                    return false;
                }
            }
        }

        return true;
    }

    public void outputBattleGrid() {
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                System.out.print(battleGrid[i][j].ordinal() + " ");
            }
            System.out.println();
        }
    }

    public void setCell(int row, int col, BATTLE_GRID_STATE state) {
        battleGrid[row][col] = state;
    }

    @Override
    public BattleGrid clone() {
        BattleGrid clone = new BattleGrid();

        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                clone.setCell(row, col, battleGrid[row][col]);
            }
        }

        return clone;
    }
}
