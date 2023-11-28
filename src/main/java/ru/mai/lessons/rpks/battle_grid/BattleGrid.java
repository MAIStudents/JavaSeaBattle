package ru.mai.lessons.rpks.battle_grid;

import ru.mai.lessons.rpks.point.Point;

import java.util.ArrayList;
import java.util.List;

import static ru.mai.lessons.rpks.client.ClientController.GRID_SIZE;

public class BattleGrid {
    public static enum BATTLE_GRID_STATE {
        OCCUPIED,
        NOT_OCCUPIED,
        HIT
    }

    private List<List<BATTLE_GRID_STATE>> battleGrid;

    public BattleGrid() {
        battleGrid = new ArrayList<>();

        for (int row = 0; row < GRID_SIZE; row++) {
            battleGrid.add(new ArrayList<>());

            for (int col = 0; col < GRID_SIZE; col++) {
                battleGrid.get(row).add(BATTLE_GRID_STATE.NOT_OCCUPIED);
            }
        }
    }

    public void setCell(int row, int col, BATTLE_GRID_STATE state) {
        battleGrid.get(row).set(col, state);
    }

    public boolean isOccupied(int row, int col) {
        return battleGrid.get(row).get(col).equals(BATTLE_GRID_STATE.OCCUPIED);
    }
}
