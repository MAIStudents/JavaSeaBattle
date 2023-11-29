package ru.mai.lessons.rpks.factory;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;

public class ButtonFactory {
    public static Button createButton(double height, double width, String backgroundColor, String borderColor, EventHandler<ActionEvent> onButton) {
        Button button = new Button();
        button.setMinHeight(height);
        button.setMinWidth(width);
        button.setMaxHeight(height);
        button.setMaxWidth(width);

        button.setStyle(
                "-fx-background-color: " + backgroundColor + ";" +
                "-fx-border-color: " + borderColor + ";" +
                "-fx-border-width: 2px;"
        );

        button.setOnAction(onButton);

        return button;
    }
}
