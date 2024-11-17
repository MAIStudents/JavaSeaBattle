package ru.mai.lessons.rpks;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class Client extends Application {

    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;
    private static Socket clientSocket;

    private static BufferedReader in;
    private static BufferedWriter out;
    private static final Alert hint = new Alert(Alert.AlertType.INFORMATION);
    private static final Alert rules = new Alert(Alert.AlertType.INFORMATION);
    private static final Alert awaitingPlayer = new Alert(Alert.AlertType.INFORMATION);

    private static final Alert winningInfo = new Alert(Alert.AlertType.CONFIRMATION);

    private static final GameController gameController = new GameController();
    private static Thread listener;
    private static Button ourBtn;
    private static Button enemyBtn;

    public void makeMove(int x, int y) {
        try {
            out.write("STEP\n");
            out.flush();
            out.write(String.format("0,%d,%d;\n", x, y));
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void start(Stage primaryStage) {

        BorderPane root = new BorderPane();

        MenuBar menuBar = new MenuBar();
        Menu helpMenu = new Menu("Помощь");
        MenuItem rulesItem = new MenuItem("Правила");
        MenuItem hintItem = new MenuItem("Как играть");

        rulesItem.setOnAction(e -> showRules());
        hintItem.setOnAction(e -> showHint());

        helpMenu.getItems().add(rulesItem);
        helpMenu.getItems().add(hintItem);

        menuBar.getMenus().add(helpMenu);
        root.setTop(menuBar);

        Scene scene = new Scene(root, 800, 450);

        GridPane playerGrid = new GridPane();

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
                playerGrid.add(cell, col, row);
            }
            gameController.buttons.add(buttons);
        }
        gameController.clearBattlefield();

        GridPane enemyGrid = new GridPane();

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
                    makeMove(finalRow, finalCol);
                    ourBtn.setText("Ход противника");
                    gameController.endMove();
                });
                enemyGrid.add(cell, col, row);
            }
            gameController.enemyButtons.add(buttons);
        }

        Label playerLabel = new Label("Своё поле");
        playerLabel.setFont(new Font(16));
        playerLabel.setAlignment(Pos.CENTER);

        Label enemyLabel = new Label("Поле врага");
        enemyLabel.setFont(new Font(16));
        enemyLabel.setAlignment(Pos.CENTER);

        Button readyButton = new Button("Готов?");
        readyButton.setOnAction(e -> tryStartGame());
        ourBtn = readyButton;

        Button enemyButton = new Button("");
        enemyButton.setDisable(true);
        enemyButton.setStyle("-fx-background-color: transparent;");
        enemyBtn = enemyButton;

        VBox playerBox = new VBox(10, playerLabel, playerGrid, readyButton);
        playerBox.setAlignment(Pos.CENTER);

        VBox enemyBox = new VBox(10, enemyLabel, enemyGrid, enemyButton);
        enemyBox.setAlignment(Pos.CENTER);

        HBox gridBox = new HBox(50, playerBox, enemyBox);
        gridBox.setAlignment(Pos.CENTER);

        root.setCenter(gridBox);
        primaryStage.setOnCloseRequest((event) -> exitProgram());

        startGame();
        primaryStage.setTitle("Pacific Fight (Battleship)");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void showRules() {
        rules.setTitle("Правила игры");
        rules.setHeaderText("Правила игры в Морской Бой");
        rules.setContentText("""
                1. Разместите свои корабли на поле.
                2. Поочередно атакуйте клетки на поле противника.
                3. Побеждает тот, кто первым потопит все корабли противника.""");
        rules.showAndWait();
    }
    private void showHint() {
        hint.setTitle("Как играть");
        hint.setHeaderText("Действия");
        hint.setContentText("""
                1. Для размещения корабля нажмите (лкм) на клетку на поле
                2. Для удаления корабля нажмите (лкм) по кораблю
                3. Для увеличения корабля нажмите на клетку рядом (лкм)""");
        hint.showAndWait();
    }


    private void tryStartGame() {
        if (!gameController.checkField()) {
            showWarningWrongShips();
        } else {
            ourBtn.setDisable(true);
            ourBtn.setText("Ждём противника");
            waitingServer();
        }
    }
    private void showWarningWrongShips() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Ошибка");
        alert.setHeaderText("Неверное число кораблей");
        alert.setContentText("""
                Должен быть :\s
                1 корабль размера 4,
                2 корабля размера 3,
                3 корабля размера 2,
                4 корабля размера 1
                """);
        alert.showAndWait();
    }

    private void showEndingOption(int type) {
        winningInfo.setTitle("Игра окончена");
        if (type == 1) {
            winningInfo.setHeaderText("Поздравляем с победой! Слава Империи!");
        } else if (type == 2) {
            winningInfo.setHeaderText("Вы обрекли свой флот на погибель.");
        } else if (type == 3) {
            winningInfo.setHeaderText("Другой игрок отключился...");
        }
        winningInfo.setContentText("Начать новую игру или выйти?");

        ButtonType newGameButton = new ButtonType("Новая игра");
        ButtonType exitButton = new ButtonType("Выход");
        winningInfo.getButtonTypes().setAll(newGameButton, exitButton);

        Platform.runLater(() -> {
            Optional<ButtonType> result = winningInfo.showAndWait();
            result.ifPresent(buttonType -> {
                if (buttonType == newGameButton) {
                    restartApplication();
                } else {
                    exitProgram();
                }
            });
        });
    }


    private void waitingServer() {
        try {
            out.write("READY\n");
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        awaitingPlayer.setTitle("К бою");
        awaitingPlayer.setHeaderText("Ждём другого игрока...");

        awaitingPlayer.getDialogPane().lookupButton(ButtonType.OK).setVisible(false);
        awaitingPlayer.show();
    }

    public void readResponse() {
        try {
            String action = in.readLine();
            System.out.printf("<%s>\n", action);

            String points;

            switch (action) {
                case "RESPONSE":
                    points = in.readLine();
                    gameController.colorPoints(GameEvent.getEvents(points), gameController.enemyButtons);
                    break;
                case "LOSE":
                    points = in.readLine();
                    gameController.colorPoints(GameEvent.getEvents(points), gameController.enemyButtons);
                    showEndingOption(1);
                    break;
                case "DICONNECT":
                    showEndingOption(3);
                    break;
                case "STEP":
                    String pos = in.readLine();
                    var resulting = gameController.enemyMakeStep(GameEvent.getEvents(pos));
                    if (resulting.second) {
                        showEndingOption(2);
                        out.write("LOSE\n");
                    } else {
                        out.write("RESPONSE\n");
                    }
                    out.flush();
                    out.write(GameEvent.listToString(resulting.first) + "\n");
                    out.flush();
                    break;
                case "TURN":
                    Platform.runLater(() -> ourBtn.setText("Ваш ход"));
                    gameController.prepareMove();
                    break;
                case "START":
                    Platform.runLater(awaitingPlayer::close);
                    break;
            }
        } catch (IOException e) {
            System.out.printf(e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
        Platform.exit();
    }
    public static void startGame() {
        try {
            gameController.clearFields();
            clientSocket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

            Client client = new Client();

            listener = new Thread(() -> {
                try {
                    while (!clientSocket.isClosed()) {
                        if (in.ready()) {
                            client.readResponse();
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    System.out.printf("Game ended\n");
                }
            });
            listener.start();

        } catch (IOException e) {
            System.err.println("Ошибка подключения: " + e.getMessage());
            e.printStackTrace();
            Platform.exit();
            throw new RuntimeException(e.getMessage());
        }
    }
    private static void closeConnections() {
        try {
            if (clientSocket != null) {
                clientSocket.close();
            }
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            if (listener != null) {
                listener.interrupt();
                listener = null;
            }
        } catch (IOException e) {
            System.out.printf(e.getMessage());
            e.printStackTrace();
        }
    }
    private static void restartApplication() {
        closeConnections();
        Platform.runLater(Client::startGame);
    }
    public static void exitProgram() {
        closeConnections();
        Platform.exit();
    }
}
