package ru.mai.lessons.rpks.battle_grid;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import ru.mai.lessons.rpks.factory.ButtonFactory;
import ru.mai.lessons.rpks.functional_interface.TwoParameterFunction;

public class BattleGridPaneUtils extends GridPane {
    public static final int GRID_SIZE = 10;

    public static void setOnActionButtons(GridPane gridPane, TwoParameterFunction function) {
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                int finalRow = row;
                int finalCol = col;
                Button button = (Button) getNodeByRowColumnIndex(row, col, gridPane);
                button.setOnAction(event -> function.apply(finalRow, finalCol));
            }
        }
    }

    public static void fillGridEmpty(GridPane gridPane) {
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                gridPane.add(ButtonFactory.createButton(
                        27,
                        27,
                        "white",
                        "black",
                        null),
                        col,
                        row);
            }
        }
    }

    public static boolean placeShip(GridPane gridPane, BattleGrid battleGrid, int rowIndex, int colIndex, int shipSize, boolean horizontal) {
        if (battleGrid.placeShip(rowIndex, colIndex, shipSize, horizontal)) {
            for (int row = 0; row < GRID_SIZE; row++) {
                for (int col = 0; col < GRID_SIZE; col++) {
                    if (battleGrid.isOccupied(row, col)) {
                        gridPane.getChildren().remove(getNodeByRowColumnIndex(row, col, gridPane));
                        gridPane.add(ButtonFactory.createButton(
                                27,
                                27,
                                "gray",
                                "black",
                                null),
                                col,
                                row);
                    }
                }
            }

            return true;
        }

        return false;
    }

    public static void resetGrid(GridPane gridPane) {
        gridPane.getChildren().clear();
        fillGridEmpty(gridPane);
    }

    public static void fillGridRandomly(GridPane gridPane, BattleGrid battleGrid) {
        resetGrid(gridPane);
        battleGrid.fillBattleGridRandomly();
        fillGrid(battleGrid, gridPane);
    }

    public static void fillGrid(BattleGrid battleGrid, GridPane gridPane) {
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                gridPane.getChildren().remove(getNodeByRowColumnIndex(row, col, gridPane));

                if (battleGrid.isOccupied(row, col)) {
                    gridPane.add(ButtonFactory.createButton(
                            27,
                            27,
                            "gray",
                            "black",
                            null),
                            col,
                            row);
                } else {
                    gridPane.add(ButtonFactory.createButton(
                            27,
                            27,
                            "white",
                            "black",
                            null),
                            col,
                            row);
                }
            }
        }
    }

    public static Node getNodeByRowColumnIndex(final int row, final int column, GridPane gridPane) {
        ObservableList<Node> children = gridPane.getChildren();

        for (Node node : children) {
            if (GridPane.getRowIndex(node) == row && GridPane.getColumnIndex(node) == column) {
                return node;
            }
        }

        return null;
    }
}
