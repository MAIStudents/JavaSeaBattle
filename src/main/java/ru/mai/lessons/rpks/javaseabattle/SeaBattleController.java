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
    private boolean turnDone;

    private AtomicInteger turnX = new AtomicInteger(-1);
    private AtomicInteger turnY = new AtomicInteger(-1);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        canMakeTurn = false;
        turnDone = false;
        for (int i = 0; i < 10; ++i) {
            for (int j = 0; j < 10; ++j) {

                int finalI = i;
                int finalJ = j;

                Button button = new Button();
                button.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
                button.setDisable(true);
                playerGrid.add(button, i, j);

                button = new Button();
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
            turnDone = true;
        }
    }

    public void setLabelText(String text) {
        label.setText(text);
    }

    public void setCanMakeTurn(boolean can) {
        canMakeTurn = can;
    }

    public String getTurn() {
        while (!turnDone) {
            try {
                Thread.sleep(10L);
            } catch (InterruptedException ignored) {}
        }
        turnDone = false;
        return "" + turnX.get() + " " + turnY.get();
    }

    public void setButtonText(String text) {
        Button button = (Button) opponentGrid.getChildren().get(turnX.get() * 10 + turnY.get());
        button.setText(text);
        button.setDisable(true);
    }

    public void setBattleships(String message) {

        String[] positions = message.split("#");

        int x0, y0, x1, y1;

        for (int i = 0; i < positions.length; ++i) {

            String[] coordinates = positions[i].split(" ");

            x0 = Integer.parseInt(coordinates[0]);
            y0 = Integer.parseInt(coordinates[1]);
            x1 = Integer.parseInt(coordinates[2]);
            y1 = Integer.parseInt(coordinates[3]);

            for (int xi = x0; xi <= x1; ++xi) {
                for (int yi = y0; yi <= y1; ++yi) {
                    ((Button) playerGrid.getChildren().get(xi * 10 + yi)).setText("#");
                }
            }
        }
    }
}