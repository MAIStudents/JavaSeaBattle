package ru.mai.lessons.rpks.javaseabattle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import ru.mai.lessons.rpks.javaseabattle.commons.IntPoint;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;

public class SeaBattleController implements Initializable {

    @FXML
    private Label label;
    @FXML
    private GridPane playerGrid;
    @FXML
    private GridPane opponentGrid;

    private boolean canMakeTurn;

    private AtomicInteger turnX = new AtomicInteger(-1);
    private AtomicInteger turnY = new AtomicInteger(-1);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        canMakeTurn = false;
        for (int i = 0; i < 10; ++i) {
            for (int j = 0; j < 10; ++j) {

                int finalI = i;
                int finalJ = j;

                Button button = new Button("ant");
                button.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
                button.setDisable(true);
                playerGrid.add(button, i, j);

                button = new Button("GI");
                button.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
                button.setOnAction((val) -> setTurnCoordinates(finalI, finalJ));
                opponentGrid.add(button, i, j);
            }
        }
    }

    public void setTurnCoordinates(int x, int y) {
        if (canMakeTurn) {
            turnX.set(x);
            turnY.set(y);
        }
    }

    public void setLabelText(String text) {
        label.setText(text);
    }

    public void setCanMakeTurn(boolean can) {
        canMakeTurn = can;
    }

    public String getTurn() {
        return "" + turnX.get() + " " + turnY.get();
    }
}