package ru.mai.lessons.rpks;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Client extends Application {

    private static Socket clientSocket;
    private static BufferedReader reader;

    private static BufferedReader in;
    private static BufferedWriter out;

    private final GameController gameController = new GameController();

    private boolean isReady = false;
    private boolean isMyTurn = false;

    @Override
    public void start(Stage primaryStage) {
        Button button = new Button("Click Me");
        button.setOnAction(e -> System.out.println("Hello, JavaFX!"));

        StackPane root = new StackPane();
        root.getChildren().add(button);

        Scene scene = new Scene(root, 1000, 500);

        GridPane grid = new GridPane();

        for (int row = 0; row < 10; row++) {
            List<Button> buttons = new ArrayList<>();
            for (int col = 0; col < 10; col++) {
                Button cell = new Button();
                buttons.add(cell);
                cell.setMinSize(30, 30);
                int finalRow = row;
                int finalCol = col;
                cell.setOnMouseClicked(event -> {
                    System.out.printf("Clicked X=%d, Y=%d\n", finalRow, finalCol);
                    if (event.getButton() == MouseButton.PRIMARY) {
                        // left click
                        gameController.addShipCell(finalRow, finalCol, cell);
                    } else if (event.getButton() == MouseButton.SECONDARY) {
                        // right click
                        gameController.removeShipCell(finalRow, finalCol, cell);
                    }
                });
                grid.add(cell, col, row);
            }
            gameController.buttons.add(buttons);
        }
        gameController.clearBattlefield();
        root.getChildren().add(grid);

        primaryStage.setTitle("Hello JavaFX with Maven");
        primaryStage.setScene(scene);
        primaryStage.show();
       // primaryStage.close();
    }


    private String getGameEvents(List<GameEvent> gameEvents) {
        StringBuilder builder = new StringBuilder();
        for (GameEvent gameEvent : gameEvents) {
            builder.append(gameEvent.toString());
        }
        return builder.toString();
    }


    public static void main(String[] args) {
        try {
            try {
                clientSocket = new Socket("localhost", 4004);
                reader = new BufferedReader(new InputStreamReader(System.in));
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

                out.flush();
                String serverWord = in.readLine();
                System.out.println(serverWord);
            } finally {
                System.out.println("Клиент был закрыт...");
                clientSocket.close();
                in.close();
                out.close();
            }
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

}
