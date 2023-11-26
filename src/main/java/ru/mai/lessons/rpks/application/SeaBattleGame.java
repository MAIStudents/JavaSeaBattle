package ru.mai.lessons.rpks.application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class SeaBattleGame extends Application {
    SeaBattleController seaBattleController;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("sea-battle-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 400, 300);
        seaBattleController = fxmlLoader.getController();
        stage.setResizable(false);
        stage.setTitle("Sea Battle");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        seaBattleController.stopGame();
    }

    public static void main(String[] args) {
        launch();
    }
}