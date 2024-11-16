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

public class Client extends Application {

    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;
    private static Socket clientSocket;

    private static BufferedReader in;
    private static BufferedWriter out;
    private static final Alert awaitingPlayer = new Alert(Alert.AlertType.INFORMATION);

    private static final Alert winningInfo = new Alert(Alert.AlertType.CONFIRMATION);

    private static final GameController gameController = new GameController();
    private static Thread listener;
    private static boolean toRestart = true;

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
        // Создание меню и добавление пункта "Правила"
        MenuBar menuBar = new MenuBar();
        Menu helpMenu = new Menu("Помощь");
        MenuItem rulesItem = new MenuItem("Правила");
        rulesItem.setOnAction(e -> showRules());
        helpMenu.getItems().add(rulesItem);
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
                    gameController.endMove();
                });
                enemyGrid.add(cell, col, row);
            }
            gameController.enemyButtons.add(buttons);
        }

        // Текстовые метки для полей
        Label playerLabel = new Label("Своё поле");
        playerLabel.setFont(new Font(16));
        playerLabel.setAlignment(Pos.CENTER);

        Label enemyLabel = new Label("Поле врага");
        enemyLabel.setFont(new Font(16));
        enemyLabel.setAlignment(Pos.CENTER);

        // Кнопка "Готов"
        Button readyButton = new Button("Готов");
        readyButton.setOnAction(e -> tryStartGame());

        Button enemyButton = new Button("Ожидаем");
        enemyButton.setDisable(true);
        enemyButton.setOnAction(e -> System.out.println("Готов"));

        // Размещение в VBox для левого и правого столбцов
        VBox playerBox = new VBox(10, playerLabel, playerGrid, readyButton);
        playerBox.setAlignment(Pos.CENTER);

        VBox enemyBox = new VBox(10, enemyLabel, enemyGrid, enemyButton);
        enemyBox.setAlignment(Pos.CENTER);

        // Помещаем обе сетки в HBox с выравниванием
        HBox gridBox = new HBox(50, playerBox, enemyBox);
        gridBox.setAlignment(Pos.CENTER);

        // Устанавливаем gridBox в центр root панели
        root.setCenter(gridBox);

        // Настройка сцены и отображение
        primaryStage.setTitle("Pacific Fight (Battleship)");
        primaryStage.setScene(scene);
        primaryStage.show();
       // primaryStage.close();
    }

    private void showRules() {
        awaitingPlayer.setTitle("Правила игры");
        awaitingPlayer.setHeaderText("Правила игры в Морской Бой");
        awaitingPlayer.setContentText("""
                1. Разместите свои корабли на поле.
                2. Поочередно атакуйте клетки на поле противника.
                3. Побеждает тот, кто первым потопит все корабли противника.""");
        awaitingPlayer.showAndWait();
    }

    private void tryStartGame() {
        if (!gameController.checkField()) {
            showWarningWrongShips();
        } else {
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

    private void showEndingOption(boolean isWin) {
        winningInfo.setTitle("Игра окончена");
        if (isWin) {
            winningInfo.setHeaderText("Поздравляем с победой! Слава Империи!");
        } else {
            winningInfo.setHeaderText("Вы обрекли свой флот на погибель.");
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
                } else if (buttonType == exitButton) {
                    toRestart = false;
                    Platform.exit();
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
                    showEndingOption(true);
                case "STEP":
                    String pos = in.readLine();
                    var resulting = gameController.enemyMakeStep(GameEvent.getEvents(pos));
                    if (resulting.second) {
                        showEndingOption(false);
                        out.write("LOSE\n");
                        out.flush();
                        out.write(GameEvent.listToString(resulting.first) + "\n");
                        out.flush();
                    } else {
                        out.write("RESPONSE\n");
                        out.flush();
                        out.write(GameEvent.listToString(resulting.first) + "\n");
                        out.flush();
                    }
                    break;
                case "TURN":
                    gameController.prepareMove();
                    break;
                case "START":
                    Platform.runLater(awaitingPlayer::close);
                    break;
            }
        } catch (IOException e) {
            System.out.printf("get error\n");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
        while (toRestart) {
            startGame();
        }
        /*  Итого осталось
        * Рестарт -> чистим всё поле и делаем startGame()
        * При присоединении врага -> готов на кнопке
        * Наш ход -> на кнопке Ожидается ход
        * Ход врага -> ждём врага
        * */
        Platform.exit();

    }
    public static void startGame() {
        try {
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
                    System.out.printf("ended\n");
                }
            });
            listener.start();

        } catch (IOException e) {
            System.err.println("Ошибка подключения: " + e.getMessage());
        } finally {
            System.out.printf("Closed connection\n");
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
                System.out.println("Клиент был закрыт...");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private void restartApplication() {
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
            e.printStackTrace();
        }
        toRestart = true;
        Platform.exit();
    }
}
