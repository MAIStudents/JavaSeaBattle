package ru.mai.lessons.rpks.client;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import ru.mai.lessons.rpks.battle_grid.BattleGrid;
import ru.mai.lessons.rpks.battle_grid.BattleGridPaneUtils;
import ru.mai.lessons.rpks.fill_grid.FillGridController;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.ResourceBundle;

import static ru.mai.lessons.rpks.battle_grid.BattleGridPaneUtils.GRID_SIZE;

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
        BattleGridPaneUtils.fillGridEmpty(gridPaneFirst);
        BattleGridPaneUtils.fillGridEmpty(gridPaneSecond);

        BattleGridPaneUtils.setOnActionButtons(gridPaneFirst, this::onClickCell);
        BattleGridPaneUtils.setOnActionButtons(gridPaneSecond, this::onClickCell);
    }

    public void onClickCell(int rowIndex, int colIndex) {
        System.out.println("Button clicked at row: " + rowIndex + ", column: " + colIndex);
    }

    public void fillGrid() {
        battleGrid.clearBattleGrid();

        if (clientId == 0) {
            BattleGridPaneUtils.fillGridRandomly(gridPaneFirst, battleGrid);
        } else {
            BattleGridPaneUtils.fillGridRandomly(gridPaneSecond, battleGrid);
        }
    }

    public void fillBySelf() throws IOException {
        Stage stageClient = new Stage();
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("fill-grid-view.fxml"));
        fxmlLoader.setController(new FillGridController(this));
        Scene scene = new Scene(fxmlLoader.load(), 600, 600);
        stageClient.setResizable(false);
        stageClient.setTitle("Sea Battle");
        stageClient.setScene(scene);
        stageClient.show();
    }

    public void fillFromBattleGrid(BattleGrid battleGridOther) {
        BattleGridPaneUtils.resetGrid(gridPaneFirst);
        battleGrid.clearBattleGrid();
        battleGrid = battleGridOther.clone();
        BattleGridPaneUtils.fillGrid(battleGridOther, gridPaneFirst);
    }
}
