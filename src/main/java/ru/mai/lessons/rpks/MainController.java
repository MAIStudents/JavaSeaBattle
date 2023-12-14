package ru.mai.lessons.rpks;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import ru.mai.lessons.rpks.Components.InitPoints;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;

import static ru.mai.lessons.rpks.Components.CellText.*;


public class MainController implements Initializable {

    @FXML
    private Label label;
    @FXML
    private GridPane playerGrid;
    @FXML
    private GridPane opponentGrid;
    @FXML
    private Label responseLabel;

    private boolean canMakeTurn;
    private boolean turnDone;
    private Background emptyCellBG;
    private Background shootCellBG;
    private Background shootShipCellBG;
    private Background shipCellBG;
    private final int MIN_CELL = 0;
    private final int MAX_CELL = 10;
    private List<InitPoints> directions;


    public enum cellState {
        empty,
        ship,
        shoot,
        shootShip
    }


    private AtomicInteger turnX = new AtomicInteger(-1);
    private AtomicInteger turnY = new AtomicInteger(-1);

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        directions = new ArrayList<>(
                Arrays.asList(
                        new InitPoints(0, 1),
                        new InitPoints(0, -1),
                        new InitPoints(1, 0),
                        new InitPoints(-1, 0)
                )
        );

        emptyCellBG = getBackground("emptyCell.png");
        shootCellBG = getBackground("shootCell.png");
        shootShipCellBG = getBackground("shootShipCell.png");
        shipCellBG = getBackground("shipCell.png");

        Button button;
        canMakeTurn = false;
        turnDone = false;
        Paint color = new Color(0,0,0,0);

        for (int i = MIN_CELL; i < MAX_CELL; ++i) {
            for (int j = MIN_CELL; j < MAX_CELL; ++j) {

                int finalI = i;
                int finalJ = j;

                button = new Button();

                button.setTextFill(color);
                button.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
                button.setDisable(true);
                button.setBackground(emptyCellBG);
                playerGrid.add(button, i, j);

                button = new Button();

                button.setTextFill(color);
                button.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);

                button.setOnAction((val) -> setTurnCoordinates(finalI, finalJ));
                button.setBackground(emptyCellBG);
                opponentGrid.add(button, i, j);
            }
        }
    }

    private Background getBackground(String filename) {
        Button button = new Button();
        Image image = new Image(MainController.class.getClassLoader().getResource(filename).toString(), button.getWidth(), button.getHeight(), false, true, true);
        BackgroundImage backgroundImage = new BackgroundImage(image, BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER,
                new BackgroundSize(button.getWidth(), button.getHeight(), true, true, true, false));

        return new Background(backgroundImage);
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

    public void setButtonAppearance(cellState state) {
        Button button = (Button) opponentGrid.getChildren().get(turnX.get() * MAX_CELL + turnY.get());

        switch (state) {
            case ship -> {
                button.setText(CELL_SHIP);
                button.setBackground(shipCellBG);
            }
            case shoot -> {
                button.setText(CELL_PAST);
                button.setBackground(shootCellBG);
            }
            case shootShip -> {
                button.setText(CELL_WOUNDED);
                button.setBackground(shootShipCellBG);
            }
            case empty -> {
                button.setText("");
                button.setBackground(emptyCellBG);
            }
        }
        button.setDisable(true);
    }

    private boolean inBounds(int from, int to, int num) {
        return from <= num && num < to;
    }

    public void surroundKilledShip(GridPane gridPane, int X, int Y) {

        int nextX = X;
        int nextY = Y;
        int dx = 0, dy = 0;
        boolean foundDirection = false;

        for (int i = 0; i < directions.size() && !foundDirection; ++i) {

            dx = directions.get(i).getX();
            dy = directions.get(i).getY();

            nextX = X + dx;
            nextY = Y + dy;

            if (!(inBounds(MIN_CELL, MAX_CELL, nextX) && inBounds(MIN_CELL, MAX_CELL, nextY))) {
                continue;
            }

            if (((Button) gridPane.getChildren().get(nextX * MAX_CELL + nextY)).getText().equals(CELL_WOUNDED)) {

                foundDirection = true;
                do {

                    nextX += dx;
                    nextY += dy;

                } while (inBounds(MIN_CELL, MAX_CELL, nextX) && inBounds(MIN_CELL, MAX_CELL, nextY) &&
                        ((Button) gridPane.getChildren().get(nextX * MAX_CELL + nextY)).getText().equals(CELL_WOUNDED));
                nextX -= dx;
                nextY -= dy;
            }
        }

        dx *= -1;
        dy *= -1;

        Button button;

        while ((inBounds(MIN_CELL, MAX_CELL, nextX) && inBounds(MIN_CELL, MAX_CELL, nextY)) &&
                ((Button) gridPane.getChildren().get(nextX * MAX_CELL + nextY)).getText().equals(CELL_WOUNDED)) {

            for (int x = nextX - 1; x <= nextX + 1; ++x) {

                if (!inBounds(MIN_CELL, MAX_CELL, x)) {
                    continue;
                }
                for (int y = nextY - 1; y <= nextY + 1; ++y) {

                    if (!inBounds(MIN_CELL, MAX_CELL, y)) {
                        continue;
                    }
                    button = (Button) gridPane.getChildren().get(x * MAX_CELL + y);
                    if (!button.getText().equals(CELL_WOUNDED)) {
                        button.setText(CELL_PAST);
                        button.setBackground(shootCellBG);
                        button.setDisable(true);
                    }
                }
            }

            nextX += dx;
            nextY += dy;
        }
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

            Button button;
            for (int xi = x0; xi <= x1; ++xi) {
                for (int yi = y0; yi <= y1; ++yi) {
                    button = (Button) playerGrid.getChildren().get(xi * MAX_CELL + yi);
                    button.setText(CELL_SHIP);
                    button.setBackground(shipCellBG);
                }
            }
        }
    }

    public void setResponseLabelText(String text) {
        responseLabel.setText(text);
    }

    public void changeButton(int x, int y) {

        Button button = (Button) playerGrid.getChildren().get(x * MAX_CELL + y);
        String text = button.getText();

        if (text.equals("#")) {
            button.setBackground(shootShipCellBG);
            button.setText("x");

            if (checkIfKilled(x, y)) {
                surroundKilledShip(playerGrid, x, y);
            }
        } else {
            button.setBackground(shootCellBG);
            button.setText(".");
        }
    }

    private boolean checkIfKilled(int ix, int iy) {

        int nextX = ix;
        int nextY = iy;
        int dx = 0, dy = 0;
        boolean foundDirection = false;

        for (int i = 0; i < directions.size() && !foundDirection; ++i) {

            dx = directions.get(i).getX();
            dy = directions.get(i).getY();

            nextX = ix + dx;
            nextY = iy + dy;

            if (!(inBounds(MIN_CELL, MAX_CELL, nextX) && inBounds(MIN_CELL, MAX_CELL, nextY))) {
                continue;
            }

            if (((Button) playerGrid.getChildren().get(nextX * MAX_CELL + nextY)).getText().equals(CELL_WOUNDED)) {

                foundDirection = true;
                do {

                    nextX += dx;
                    nextY += dy;

                    if (!(inBounds(MIN_CELL, MAX_CELL, nextX) && inBounds(MIN_CELL, MAX_CELL, nextY))) {
                        nextX -= dx;
                        nextY -= dy;
                        break;
                    }

                } while (((Button) playerGrid.getChildren().get(nextX * MAX_CELL + nextY)).getText().equals(CELL_WOUNDED));
            }

            if (((Button) playerGrid.getChildren().get(nextX * MAX_CELL + nextY)).getText().equals(CELL_SHIP)) {
                return false;
            }
        }

        dx *= -1;
        dy *= -1;

        do {

            nextX += dx;
            nextY += dy;

            if (!(inBounds(MIN_CELL, MAX_CELL, nextX) && inBounds(MIN_CELL, MAX_CELL, nextY))) {
                nextX -= dx;
                nextY -= dy;
                break;
            }

        } while (((Button) playerGrid.getChildren().get(nextX * MAX_CELL + nextY)).getText().equals(CELL_WOUNDED));

        return !((Button) playerGrid.getChildren().get(nextX * MAX_CELL + nextY)).getText().equals(CELL_SHIP);
    }

    public GridPane getOpponentGrid() {
        return opponentGrid;
    }

    public int getLastTurnX() {
        return turnX.get();
    }

    public int getLastTurnY() {
        return turnY.get();
    }

    public void shutdown() {
        turnDone = true;
    }
}