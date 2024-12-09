package ru.mai.lessons.rpks.Game.Logic;

import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import ru.mai.lessons.rpks.Game.Field.Field;

import java.util.logging.Level;
import java.util.logging.Logger;

import static ru.mai.lessons.rpks.Game.Game.SHIP_STYLE;

public class ShipPlacement {
    private static final Logger logger = Logger.getLogger(ShipPlacement.class.getName());

    public static String getShipsInfo(Field field) {
        return "Однопалубные: " + (Field.ONE_DECK_SHIPS - field.currentOneDeckShips) + "/4\nДвухпалубные: " +
                (Field.TWO_DECK_SHIPS - field.currentTwoDeckShips) + "/3\nТрехпалубные: " +
                (Field.THREE_DECK_SHIPS - field.currentThreeDeckShips) + "/2\nЧетырехпалубные: " +
                (Field.FOUR_DECK_SHIPS - field.currentFourDeckShips) + "/1";
    }

    public static GridPane getGridPane(Field field, Text textArea) {
        GridPane grid = new GridPane();
        grid.setPrefSize(300, 300);
        grid.setHgap(2);
        grid.setVgap(2);
        for (int i = 0; i < field.getSize(); ++i) {
            for (int j = 0; j < field.getSize(); ++j) {
                Button cell = getCell(field, textArea, i, j);
                grid.add(cell, j, i);
            }
        }
        return grid;
    }

    private static Button getCell(Field field, Text textArea, int x, int y) {
        Button cell = new Button();
        cell.setMinSize(30, 30);
        cell.setOnAction(event -> {
            if (!field.isShipAt(x, y) && field.placePart(x, y)) {
                cell.setStyle(SHIP_STYLE);
                textArea.setText(getShipsInfo(field));
            } else if (field.isShipAt(x, y)) {
                field.getCell(x, y).setShip(false);
                field.updateShipsSizes(x, y);
                cell.setStyle("");
                textArea.setText(getShipsInfo(field));
            } else {
                logger.log(Level.WARNING, "Invalid placement");
            }
        });
        return cell;
    }
}
