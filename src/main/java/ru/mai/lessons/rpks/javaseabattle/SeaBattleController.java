package ru.mai.lessons.rpks.javaseabattle;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class SeaBattleController {
    @FXML
    private Label welcomeText;

    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("Welcome to JavaFX Application!");
    }
}