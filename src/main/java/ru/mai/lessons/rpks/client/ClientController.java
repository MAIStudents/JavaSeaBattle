package ru.mai.lessons.rpks.client;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import ru.mai.lessons.rpks.battle_grid.BattleGrid;
import ru.mai.lessons.rpks.factory.ButtonFactory;

import java.net.URL;
import java.util.*;

public class ClientController implements Initializable {
    @FXML
    private GridPane gridPaneFirst;
    @FXML
    private GridPane gridPaneSecond;
    private BattleGrid battleGrid;
    public static final int GRID_SIZE = 10;
    private static final int[] SHIP_SIZES = {4, 3, 3, 2, 2, 2, 1, 1, 1, 1};
    private int clientId;

    public ClientController(int clientId) {
        this.clientId = clientId;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        battleGrid = new BattleGrid();

        if (clientId == 0) {
            fillGrid(gridPaneFirst, true);
            fillGrid(gridPaneSecond, false);
        } else {
            fillGrid(gridPaneFirst, false);
            fillGrid(gridPaneSecond, true);
        }
    }

    public void onClickCell() {

    }

    private void fillGrid(GridPane gridPane, boolean withShips) {
        if (withShips) {
            placeShips(gridPane);

            for (int row = 0; row < GRID_SIZE; row++) {
                for (int col = 0; col < GRID_SIZE; col++) {
                    if (!battleGrid.isOccupied(row, col)) {
                        gridPane.add(ButtonFactory.createButton(
                                        27,
                                        27,
                                        "white",
                                        "black",
                                        actionEvent -> onClickCell()),
                                row,
                                col);
                    }
                }
            }
        } else {
            for (int row = 0; row < GRID_SIZE; row++) {
                for (int col = 0; col < GRID_SIZE; col++) {
                    gridPane.add(ButtonFactory.createButton(
                            27,
                            27,
                            "white",
                            "black",
                            actionEvent -> onClickCell()),
                            row,
                            col);
                }
            }
        }
    }

    private void placeShips(GridPane gridPane) {
        clearGridPane(gridPane);
        List<Integer> shipSizes = new ArrayList<>();

        for (int size : SHIP_SIZES) {
            shipSizes.add(size);
        }

        Collections.shuffle(shipSizes);
        placeShipsRandomly(gridPane, shipSizes);
    }

    private void clearGridPane(GridPane gridPane) {
        gridPane.getChildren().clear();
    }

    private void placeShipsRandomly(GridPane gridPane, List<Integer> shipSizes) {
        Random random = new Random();

        for (Integer shipSize : shipSizes) {
            boolean horizontal = random.nextBoolean();
            int row = random.nextInt(GRID_SIZE);
            int col = random.nextInt(GRID_SIZE);

            while (!isCellAvailable(gridPane, row, col, shipSize, horizontal)) {
                row = random.nextInt(GRID_SIZE);
                col = random.nextInt(GRID_SIZE);
            }

            if (horizontal) {
                for (int i = 0; i < shipSize; i++) {
                    gridPane.add(ButtonFactory.createButton(
                            27,
                            27,
                            "gray",
                            "black",
                            actionEvent -> onClickCell()),
                            col + i,
                            row);
                    battleGrid.setCell(col + i, row, BattleGrid.BATTLE_GRID_STATE.OCCUPIED);
                }
            } else {
                for (int i = 0; i < shipSize; i++) {
                    gridPane.add(ButtonFactory.createButton(
                            27,
                            27,
                            "gray",
                            "black",
                            actionEvent -> onClickCell()),
                            col,
                            row + i);
                    battleGrid.setCell(col, row + i, BattleGrid.BATTLE_GRID_STATE.OCCUPIED);
                }
            }
        }
    }

    private boolean isCellAvailable(GridPane gridPane, int startRow, int startCol, int shipSize, boolean horizontal) {
        if (horizontal) {
            if (startCol + shipSize >= GRID_SIZE) {
                return false;
            }

            for (int i = startCol; i < startCol + shipSize; i++) {
                if (isCellEmpty(gridPane, startRow, i) || isAdjacentCellEmpty(gridPane, startRow, i)) {
                    return false;
                }
            }
        } else {
            if (startRow + shipSize >= GRID_SIZE) {
                return false;
            }

            for (int i = startRow; i < startRow + shipSize; i++) {
                if (isCellEmpty(gridPane, i, startCol) || isAdjacentCellEmpty(gridPane, i, startCol)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isCellEmpty(GridPane gridPane, int row, int col) {
        return gridPane.getChildren().stream()
                .anyMatch(node -> GridPane.getRowIndex(node) != null &&
                        GridPane.getRowIndex(node) == row &&
                        GridPane.getColumnIndex(node) != null &&
                        GridPane.getColumnIndex(node) == col);
    }

    private boolean isAdjacentCellEmpty(GridPane gridPane, int row, int col) {
        int checkStartRow = Math.max(0, row - 1);
        int checkStartCol = Math.max(0, col - 1);
        int checkEndRow = Math.min(GRID_SIZE - 1, row + 1);
        int checkEndCol = Math.min(GRID_SIZE - 1, col + 1);

        for (int i = checkStartRow; i <= checkEndRow; i++) {
            for (int j = checkStartCol; j <= checkEndCol; j++) {
                if (isCellEmpty(gridPane, i, j)) {
                    return true;
                }
            }
        }

        return false;
    }
}
