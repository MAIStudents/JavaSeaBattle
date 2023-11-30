package ru.mai.lessons.rpks.client;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import org.apache.log4j.Logger;
import ru.mai.lessons.rpks.battle_grid.BattleGrid;
import ru.mai.lessons.rpks.battle_grid.BattleGridPaneUtils;
import ru.mai.lessons.rpks.fill_grid.FillGridController;
import ru.mai.lessons.rpks.point.Point;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class ClientController implements Initializable {
    private static final Logger logger = Logger.getLogger(ClientController.class.getName());
    @FXML
    private Label labelStep;
    @FXML
    private Button readyButton;
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
    private boolean step = false;
    private Stage stage;

    public ClientController(BattleGrid battleGrid, int clientId, Client client, Stage stage) {
        this.battleGrid = battleGrid;
        this.clientId = clientId;
        this.client = client;
        this.stage = stage;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        BattleGridPaneUtils.fillGridEmpty(gridPaneFirst);
        BattleGridPaneUtils.fillGridEmpty(gridPaneSecond);

        BattleGridPaneUtils.setOnActionButtons(gridPaneFirst, this::onClickCell);
        BattleGridPaneUtils.setOnActionButtons(gridPaneSecond, this::onClickCell);
    }

    public void onClickCell(int rowIndex, int colIndex) {
        if (step) {
            System.out.println("Button clicked at row: " + rowIndex + ", column: " + colIndex);
            client.sendShootMessage(clientId, "", "SHOOT", rowIndex, colIndex);
            client.setStep(false);
            step = false;
        }
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
            readyButton.setDisable(true);
            client.setReady(true);
            client.sendSimpleMessage("", "READY");
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

    public void setStep(boolean step) {
        this.step = step;
    }

    public void setLabelSet(boolean step) {
        if (step) {
            labelStep.setText("ВАШ ХОД");
        } else {
            labelStep.setText("ХОД ПРОТИВНИКА");
        }
    }

    public void setHitFirst(int row, int col) {
        Button buttonHit = (Button) BattleGridPaneUtils.getNodeByRowColumnIndex(row, col, gridPaneFirst);
        buttonHit.setDisable(true);
        buttonHit.setStyle("-fx-background-color: #808080;");
    }

    public void setHitSecond(int row, int col) {
        Button buttonHit = (Button) BattleGridPaneUtils.getNodeByRowColumnIndex(row, col, gridPaneSecond);
        buttonHit.setDisable(true);
        buttonHit.setStyle("-fx-background-color: #808080;");
    }

    public void setHitEnemy(int row, int col) {
        Button buttonHit = null;

        if (clientId == 1) {
            buttonHit = (Button) BattleGridPaneUtils.getNodeByRowColumnIndex(row, col, gridPaneSecond);
        } else {
            buttonHit = (Button) BattleGridPaneUtils.getNodeByRowColumnIndex(row, col, gridPaneFirst);
        }

        buttonHit.setDisable(true);
        buttonHit.setStyle("-fx-background-color: #808080;");
    }

    public void win() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Вы выиграли!");
        alert.showAndWait();
        stage.close();

    }

    public void lose() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Вы проиграли!");
        alert.showAndWait();
        stage.close();
    }

    public void enemyDisconnect() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Противник отключился, игра прервана!");
        alert.showAndWait();
        stage.close();
    }

    public void setNotHitMain(int row, int col) {
        Button buttonMissed = null;

        if (clientId == 1) {
            buttonMissed = (Button) BattleGridPaneUtils.getNodeByRowColumnIndex(row, col, gridPaneFirst);
        } else {
            buttonMissed = (Button) BattleGridPaneUtils.getNodeByRowColumnIndex(row, col, gridPaneSecond);
        }

        buttonMissed.setDisable(true);
        buttonMissed.setStyle("-fx-background-color: red;");
    }

    public void setNotHitEnemy(int row, int col) {
        Button buttonMissed = null;

        if (clientId == 1) {
            buttonMissed = (Button) BattleGridPaneUtils.getNodeByRowColumnIndex(row, col, gridPaneSecond);
        } else {
            buttonMissed = (Button) BattleGridPaneUtils.getNodeByRowColumnIndex(row, col, gridPaneFirst);
        }

        buttonMissed.setDisable(true);
        buttonMissed.setStyle("-fx-background-color: red;");
    }
}
