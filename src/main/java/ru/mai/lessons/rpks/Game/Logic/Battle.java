package ru.mai.lessons.rpks.Game.Logic;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import ru.mai.lessons.rpks.Game.Field.Field;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static ru.mai.lessons.rpks.Game.Game.*;

public class Battle {
    private static final Logger logger = Logger.getLogger(Battle.class.getName());

    public static GridPane getFieldGridPane() {
        GridPane grid = new GridPane();
        grid.setPrefSize(300, 300);
        grid.setHgap(2);
        grid.setVgap(2);
        for (int i = 0; i < field.getSize(); ++i) {
            for (int j = 0; j < field.getSize(); ++j) {
                Button cell = getCell(i, j);
                grid.add(cell, j, i);
            }
        }
        return grid;
    }

    public static GridPane getOpponentGridPane() {
        GridPane grid = new GridPane();
        grid.setPrefSize(300, 300);
        grid.setHgap(2);
        grid.setVgap(2);
        for (int i = 0; i < field.getSize(); ++i) {
            for (int j = 0; j < field.getSize(); ++j) {
                Button cell = getOpponentCell(i, j);
                grid.add(cell, j, i);
            }
        }
        return grid;
    }

    private static Button getCell(int x, int y) {
        Button cell = new Button();
        cell.setMinSize(30, 30);
        checkStatus(cell, field, x, y);
        return cell;
    }

    private static Button getOpponentCell(int x, int y) {
        Button cell = new Button();
        cell.setMinSize(30, 30);
        checkStatus(cell, opponentField, x, y);
        cell.setOnAction(event -> {
            if (clientThread.isTurn && !opponentField.getCell(x, y).isShot() && !opponentField.getCell(x, y).isMiss()) {
                try {
                    clientThread.sendMessage(SHOT + " " + x + " " + y + "\n");
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Error while sending message", e);
                }
            }
        });
        return cell;
    }

    private static void checkStatus(Button cell, Field field, int x, int y) {
        if (field.getCell(x, y).isShot()) {
            cell.setStyle(SHOT_SHIP_STYLE);
        } else if (field.isShipAt(x, y)) {
            cell.setStyle(SHIP_STYLE);
        } else if (field.getCell(x, y).isMiss()) {
            cell.setStyle(MISS_STYLE);
        }
    }

    public static void setShotOnField(int x, int y) {
        field.setShot(x, y);
        updateCellStyle(playerGrid, field, x, y);
    }

    public static void setShotOnOpponentField(int x, int y) {
        opponentField.setShot(x, y);
        updateCellStyle(opponentGrid, opponentField, x, y);
    }

    public static void setMissOnField(int x, int y) {
        field.setMiss(x, y);
        updateCellStyle(playerGrid, field, x, y);
    }

    public static void setMissOnOpponentField(int x, int y) {
        opponentField.setMiss(x, y);
        updateCellStyle(opponentGrid, opponentField, x, y);
    }

    private static void updateCellStyle(GridPane grid, Field field, int x, int y) {
        Button cell = (Button) getNodeByRowColumnIndex(x, y, grid);
        checkStatus(cell, field, x, y);
    }

    private static Node getNodeByRowColumnIndex(final int row, final int column, GridPane grid) {
        Node result = null;
        for (Node node : grid.getChildren()) {
            if (GridPane.getRowIndex(node) == row && GridPane.getColumnIndex(node) == column) {
                result = node;
                break;
            }
        }
        return result;
    }

}
