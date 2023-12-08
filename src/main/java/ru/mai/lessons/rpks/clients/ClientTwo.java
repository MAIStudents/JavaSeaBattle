package ru.mai.lessons.rpks.clients;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClientTwo extends Application {
    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(clientAppExample.class.getResource("client.fxml"));
        ClientController clientController = new ClientController();
        fxmlLoader.setController(clientController);
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Sea battle");
        stage.setScene(scene);
        stage.show();
//        stage.setOnCloseRequest(() -> clientController.toSetOnClose());
    }
}
