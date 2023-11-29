package ru.mai.lessons.rpks.client;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import org.apache.log4j.Logger;
import ru.mai.lessons.rpks.battle_grid.BattleGrid;
import ru.mai.lessons.rpks.battle_grid.BattleGridPaneUtils;
import ru.mai.lessons.rpks.fill_grid.FillGridController;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class ClientController implements Initializable {
    private static final Logger logger = Logger.getLogger(ClientController.class.getName());
    @FXML
    private Label labelEnemyReady;
    @FXML
    private GridPane gridPaneFirst;
    @FXML
    private GridPane gridPaneSecond;
    @FXML
    private Menu gameMenu;
    private BattleGrid battleGrid;
    private final int clientId;
    private final Client client;

    public ClientController(BattleGrid battleGrid, int clientId, Client client) {
        this.battleGrid = battleGrid;
        this.clientId = clientId;
        this.client = client;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
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

        if (clientId == 1) {
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



    public void readyButtonClick() {
        if (battleGrid.isBattleGridReady()) {
            client.setReady(true);
            client.sendMessage("", "READY");
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Поле нужно заполнить до конца!");
            alert.showAndWait();
        }
    }

    public void beginGame() {
        labelEnemyReady.setText("Игра началась");
        for (MenuItem itemMenu: gameMenu.getItems()) {
            itemMenu.setDisable(true);
        }
    }
}
