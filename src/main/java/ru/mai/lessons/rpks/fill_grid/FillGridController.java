package ru.mai.lessons.rpks.fill_grid;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.GridPane;
import ru.mai.lessons.rpks.battle_grid.BattleGrid;
import ru.mai.lessons.rpks.battle_grid.BattleGridPaneUtils;
import ru.mai.lessons.rpks.client.ClientController;

import java.net.URL;
import java.util.ResourceBundle;

public class FillGridController implements Initializable {
    @FXML
    private GridPane gridPane;
    @FXML
    private ComboBox<String> comboBoxShips;
    @FXML
    private ComboBox<String> comboBoxDirection;
    private final ClientController clientController;
    private int sizeCurrentShip;
    private boolean horizontal;
    private final int[] countDeckShips = new int[] {0, 4, 3, 2, 1};
    BattleGrid battleGrid;

    public FillGridController(ClientController clientController) {
        this.clientController = clientController;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        battleGrid = new BattleGrid();
        BattleGridPaneUtils.fillGridEmpty(gridPane);

        comboBoxShips.setItems(FXCollections.observableArrayList(
                "Корабль из 4-х клеток",
                "Корабль из 3-х клеток",
                "Корабль из 2-х клеток",
                "Корабль из 1-й клетки"));
        comboBoxDirection.setItems(FXCollections.observableArrayList(
                "Горизонтально",
                "Вертикально"));

        comboBoxShips.setOnAction(event -> {
            switch (comboBoxShips.getValue()) {
                case "Корабль из 4-х клеток" -> sizeCurrentShip = 4;
                case "Корабль из 3-х клеток" -> sizeCurrentShip = 3;
                case "Корабль из 2-х клеток" -> sizeCurrentShip = 2;
                case "Корабль из 1-й клетки" -> sizeCurrentShip = 1;
            }
        });

        comboBoxDirection.setOnAction(event -> {
            switch (comboBoxDirection.getValue()) {
                case "Горизонтально" -> horizontal = true;
                case "Вертикально" -> horizontal = false;
            }
        });

        BattleGridPaneUtils.setOnActionButtons(gridPane, this::onClickCell);
    }

    private void onClickCell(int row, int col) {
        if (countDeckShips[sizeCurrentShip] > 0) {
            if (BattleGridPaneUtils.placeShip(gridPane, battleGrid, row, col, sizeCurrentShip, horizontal)) {
                countDeckShips[sizeCurrentShip]--;
            }
        }
    }

    public void loadGrid() {
        clientController.fillFromBattleGrid(battleGrid);
    }
}
