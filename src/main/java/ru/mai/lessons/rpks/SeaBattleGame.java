package ru.mai.lessons.rpks;

import javafx.application.Application;
import javafx.stage.Stage;
import ru.mai.lessons.rpks.clients.Client;

public class SeaBattleGame extends Application {
    public static void main(String[] args) {
//        Server server = new Server();
//        server.start();

        Client client = new Client("localhost", 8843);
        client.start();

        Client client2 =  new Client("localhost", 8843);
        client2.start();
        launch();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

    }
}
