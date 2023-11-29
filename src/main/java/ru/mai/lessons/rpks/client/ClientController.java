package ru.mai.lessons.rpks.client;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import ru.mai.lessons.rpks.battle_grid.BattleGrid;
import ru.mai.lessons.rpks.factory.ButtonFactory;

import java.net.URL;
import java.util.*;

import static ru.mai.lessons.rpks.battle_grid.BattleGrid.GRID_SIZE;

public class ClientController implements Initializable {
    @FXML
    private GridPane gridPaneFirst;
    @FXML
    private GridPane gridPaneSecond;
    private BattleGrid battleGrid;
    private final int clientId;

    public ClientController(int clientId) {
        this.clientId = clientId;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        battleGrid = new BattleGrid();

        gridPaneFirst.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getTarget() instanceof Button clickedButton) {
                onClickCell(GridPane.getRowIndex(clickedButton), GridPane.getColumnIndex(clickedButton));
            }
        });

        gridPaneSecond.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getTarget() instanceof Button clickedButton) {
                onClickCell(GridPane.getRowIndex(clickedButton), GridPane.getColumnIndex(clickedButton));
            }
        });

        if (clientId == 0) {
            fillGrid(gridPaneFirst, true);
            fillGrid(gridPaneSecond, false);
        } else {
            fillGrid(gridPaneFirst, false);
            fillGrid(gridPaneSecond, true);
        }
    }

    public void onClickCell(int rowIndex, int colIndex) {
        System.out.println("Button clicked at row: " + rowIndex + ", column: " + colIndex);
    }

    private void fillGrid(GridPane gridPane, boolean withShips) {

        if (withShips) {
            battleGrid.fillBattleGridRandomly();

            for (int row = 0; row < GRID_SIZE; row++) {
                for (int col = 0; col < GRID_SIZE; col++) {
                    if (battleGrid.isOccupied(row, col)) {
                        gridPane.add(ButtonFactory.createButton(
                                27,
                                27,
                                "gray",
                                "black",
                                null),
                                row,
                                col);
                    } else {
                        gridPane.add(ButtonFactory.createButton(
                                27,
                                27,
                                "white",
                                "black",
                                null),
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
                            null),
                            row,
                            col);
                }
            }
        }
    }
}
