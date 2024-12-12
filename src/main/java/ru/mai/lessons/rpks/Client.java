package ru.mai.lessons.rpks;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import ru.mai.lessons.rpks.client.GameController;
import ru.mai.lessons.rpks.logger.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class Client extends Application {
    private Button ourBtn;
    private Button enemyBtn;

    private Logger logger = new Logger(Client.class);
    private GameController gameController = new GameController();

    public static void main(String[] args) {
        launch(args);
        Platform.exit();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        if (!gameController.isCorrect()) {
            showBadServer();
            exitProgram();
        }

        BorderPane root = new BorderPane();

        MenuBar menuBar = new MenuBar();
        root.setTop(menuBar);

        Scene scene = new Scene(root, 800, 450);


        GridPane playerGrid = new GridPane();

        initMyField(playerGrid);

        GridPane enemyGrid = new GridPane();

        initEnemyField(enemyGrid);

        Label playerLabel = new Label("Your field");
        playerLabel.setFont(new Font(16));
        playerLabel.setAlignment(Pos.CENTER);

        Label enemyLabel = new Label("Enemy`s field");
        enemyLabel.setFont(new Font(16));
        enemyLabel.setAlignment(Pos.CENTER);

        Button readyButton = new Button("Are you ready?");
        readyButton.setOnAction(e -> gameController.tryStartGame());
        ourBtn = readyButton;

        Button enemyButton = new Button("Restart");
//        enemyButton.setDisable(true);
//        enemyButton.setStyle("-fx-background-color: transparent;");
        enemyButton.setOnAction(e -> restartApplication());
        enemyBtn = enemyButton;

        gameController.setEnemyBtn(enemyButton);
        gameController.setMyBtn(ourBtn);

        VBox playerBox = new VBox(10, playerLabel, playerGrid, readyButton);
        playerBox.setAlignment(Pos.CENTER);

        VBox enemyBox = new VBox(10, enemyLabel, enemyGrid, enemyButton);
        enemyBox.setAlignment(Pos.CENTER);

        HBox gridBox = new HBox(50, playerBox, enemyBox);
        gridBox.setAlignment(Pos.CENTER);

        root.setCenter(gridBox);

        primaryStage.setOnCloseRequest(event -> {
            Alert confirmExit = new Alert(Alert.AlertType.CONFIRMATION);
            confirmExit.setTitle("Exit from game");
            confirmExit.setHeaderText("Are you sure to exit?");
            confirmExit.setContentText("Game will be interrupted.");

            Optional<ButtonType> result = confirmExit.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                exitProgram();
            } else {
                event.consume();
            }
        });


        startGame();
        primaryStage.setTitle("Naval battle");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void initMyField(GridPane playerGrid) {
        for (int row = 0; row < 10; ++row) {
            List<Button> buttons = new ArrayList<>();

            for (int col = 0; col < 10; ++col) {
                Button cell = new Button();
                buttons.add(cell);
                cell.setMinSize(30, 30);

                int finalRow = row;
                int finalCol = col;

                cell.setOnMouseClicked(event -> {
                    logger.info("Clicked X={}, Y={}\n", finalRow, finalCol);
                    if (event.getButton() == MouseButton.PRIMARY) {
                        // left click
                        gameController.addShipOnCell(finalRow, finalCol, cell);
                    } else if (event.getButton() == MouseButton.SECONDARY) {
                        // right click
                        gameController.removeShipFromCell(finalRow, finalCol, cell);
                    }
                });

                playerGrid.add(cell, col, row);
            }
            gameController.addMyButtonRow(buttons);
        }
    }

    private void initEnemyField(GridPane enemyGrid) {
        for (int row = 0; row < 10; row++) {
            List<Button> buttons = new ArrayList<>();
            for (int col = 0; col < 10; col++) {
                Button cell = new Button();
                buttons.add(cell);
                cell.setMinSize(30, 30);
                int finalRow = row;
                int finalCol = col;
                cell.setDisable(true);
                cell.setOnMouseClicked(event -> {
                    if (gameController.isCellCanBeAttacked(finalRow, finalCol)) {
                        gameController.makeMove(finalRow, finalCol);
                        ourBtn.setText("Ход противника");
                        gameController.disableEnemyField();
                    }

                });
                enemyGrid.add(cell, col, row);
            }
            gameController.addEnemyButtonRow(buttons);
        }
    }

    private void showBadServer() {
        Alert err = new Alert(Alert.AlertType.ERROR);
        err.setTitle("Bad connection");
        err.setContentText("No response from server");
        err.showAndWait();
    }

    private void startGame() {
        gameController.clearFields();
        ourBtn.setDisable(false);
        ourBtn.setText("Are you ready?");
    }

    private void restartApplication() {
        gameController.updateConnection();
        if (!gameController.isCorrect()) {
            showBadServer();
            exitProgram();
        }
        Platform.runLater(this::startGame);
    }

    void exitProgram() {
        gameController.closeConnections();
        Platform.exit();
    }
}
