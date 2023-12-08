package ru.mai.lessons.rpks.clients;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class clientAppExample extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(clientAppExample.class.getResource("client.fxml"));
        fxmlLoader.setController(new ClientController());
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Sea battle");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
