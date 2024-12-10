package ru.mai.lessons.rpks.Client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import ru.mai.lessons.rpks.controllers.GameController;
import ru.mai.lessons.rpks.controllers.MessageController;
import ru.mai.lessons.rpks.include.GameEvent;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ClientController extends Application {

    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int SERVER_PORT = 8080;
    private static Socket clientSocket;

    private static BufferedReader inputStream;
    private static BufferedWriter outputStream
            ;
    private static Alert waitingAlert;
    private static Alert victoryAlert;

    private static final GameController gameController = new GameController();
    private static Thread serverListenerThread;
    private static Button readyButton;

    @Override
    public void start(Stage stage) {
        BorderPane rootLayout = createRootLayout();

        Image backgroundImage = new Image(getClass().getResource("/images/background.jpg").toExternalForm());
        BackgroundImage background = new BackgroundImage(
                backgroundImage,
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                new BackgroundSize(
                        100, 100, true, true, true, true
                )
        );
        rootLayout.setBackground(new Background(background));

        Scene mainScene = new Scene(rootLayout, 1400, 700);
        mainScene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());

        setupCloseConfirmation(stage);

        stage.setTitle("Sea Battle");
        stage.setScene(mainScene);
        stage.show();

        startGame();
    }

    public static void main(String[] args) {
        launch(args);
        Platform.exit();
    }

    private void setupCloseConfirmation(Stage stage) {
        stage.setOnCloseRequest(event -> {
            if (!showExitConfirmation()) {
                event.consume();
            } else {
                exitProgram();
            }
        });
    }

    private boolean showExitConfirmation() {
        Alert exitAlert = new Alert(Alert.AlertType.CONFIRMATION);
        exitAlert.setTitle("Exit Game");
        exitAlert.setHeaderText("Are you sure you want to exit?");
        exitAlert.setContentText("The game will be terminated.");

        Optional<ButtonType> result = exitAlert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private BorderPane createRootLayout() {
        BorderPane root = new BorderPane();

        Button menuBar = createHelpMenu();
        root.setTop(menuBar);

        HBox battlefieldLayout = createBattlefieldLayout();
        root.setCenter(battlefieldLayout);

        readyButton = createReadyButton();
        root.setBottom(readyButton);

        BorderPane.setMargin(readyButton, new Insets(10, 0, 20, 0));

        waitingAlert = new Alert(Alert.AlertType.INFORMATION);
        victoryAlert = new Alert(Alert.AlertType.CONFIRMATION);

        return root;
    }

    private Button createReadyButton() {
        readyButton = new Button("Let's GO!");
        readyButton.setOnAction(e -> start());
        BorderPane.setAlignment(readyButton, Pos.CENTER);
        return readyButton;
    }

    private HBox createBattlefieldLayout() {
        VBox playerField = createPlayerField();
        VBox enemyField = createEnemyField();

        HBox battlefieldLayout = new HBox(50, playerField, enemyField);
        battlefieldLayout.setAlignment(Pos.CENTER);

        return battlefieldLayout;
    }

    private VBox createEnemyField() {
        Label enemyLabel = createLabel("Поле врага");
        GridPane enemyGrid = createEnemyGrid();

        VBox enemyField = new VBox(10, enemyLabel, enemyGrid);
        enemyField.setAlignment(Pos.CENTER);

        return enemyField;
    }

    private VBox createPlayerField() {
        Label playerLabel = createLabel("Своё поле");
        GridPane playerGrid = createPlayerGrid();

        VBox playerField = new VBox(10, playerLabel, playerGrid);
        playerField.setAlignment(Pos.CENTER);

        return playerField;
    }

    private Label createLabel(String text) {
        Label label = new Label(text);
        label.setFont(new Font(16));
        label.setAlignment(Pos.CENTER);

        return label;
    }

    private Button createHelpMenu() {
        Button help = new Button("INFO");
        help.setOnAction(e -> showRulesAndInfo());

        return help;
    }

    private GridPane createPlayerGrid() {
        GridPane playerGrid = new GridPane();
        String imageUrl = getClass().getResource("/images/sea.jpg").toExternalForm();
        System.out.println("Image URL: " + imageUrl);
        for (int row = 0; row < 10; row++) {
            List<Button> buttons = new ArrayList<>();

            for (int col = 0; col < 10; col++) {
                Button cellButton = new Button();
                cellButton.setMinSize(30, 30);

                int finalRow = row;
                int finalCol = col;

                cellButton.setOnMouseClicked(event -> {
                    handlePlayerGridClick(finalRow, finalCol, cellButton, event);
                });

                buttons.add(cellButton);
                playerGrid.add(cellButton, col, row);
            }

            gameController.buttons.add(buttons);
        }

        gameController.clearBattlefield();

        return playerGrid;
    }

    private GridPane createEnemyGrid() {
        GridPane enemyGrid = new GridPane();

        for (int row = 0; row < 10; row++) {
            List<Button> buttons = new ArrayList<>();

            for (int col = 0; col < 10; col++) {
                Button cellButton = new Button();
                cellButton.setMinSize(30, 30);
                cellButton.setDisable(true);

                int finalRow = row;
                int finalCol = col;

                cellButton.setOnMouseClicked(event -> handleEnemyGridClick(finalRow, finalCol));

                buttons.add(cellButton);
                enemyGrid.add(cellButton, col, row);
            }

            gameController.enemyButtons.add(buttons);
        }

        return enemyGrid;
    }

    private void handlePlayerGridClick(int row, int col, Button cellButton, MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY) {
            gameController.addShipCell(row, col);
        } else if (event.getButton() == MouseButton.SECONDARY) {
            gameController.removeShipCell(row, col, cellButton);
        }
    }

    private void handleEnemyGridClick(int row, int col) {
        if (gameController.isCellCanBeAttacked(row, col)) {
            makeMove(row, col);
            readyButton.setText("Ход противника");
            gameController.endMove();
        }
    }

    private void showRulesAndInfo() {
        Stage rulesStage = new Stage();
        rulesStage.setTitle("Rules and Information");

        String rulesText = """
            Welcome to Battleship Game!

            Rules:
            1. Place your ships on the grid.
            2. Take turns attacking the opponent's grid by selecting cells.
            3. The first player to sink all opponent's ships wins.

            About the Game:
            Battleship is a classic two-player strategy game. Originally played on paper,
            it has evolved into a beloved board game and digital adaptation. 
            Players use logic and strategy to locate and destroy enemy ships.
            
            Have fun and good luck!
            """;

        TextArea textArea = new TextArea(rulesText);
        textArea.setWrapText(true);
        textArea.setEditable(false);

        VBox layout = new VBox();
        VBox.setVgrow(textArea, Priority.ALWAYS);
        layout.getChildren().add(textArea);
        layout.setPadding(new Insets(10));

        Scene scene = new Scene(layout, 400, 300);
        rulesStage.setScene(scene);

        rulesStage.initModality(Modality.APPLICATION_MODAL);

        rulesStage.showAndWait();
    }


    private void start() {
        if (!gameController.checkField()) {
            showWarningWrongShips();
        } else {
            readyButton.setDisable(true);
            readyButton.setVisible(false);
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
        victoryAlert.setTitle("Игра окончена");
        if (type == 1) {
            victoryAlert.setHeaderText("Поздравляем с победой! Слава Империи!");
        } else if (type == 2) {
            victoryAlert.setHeaderText("Вы обрекли свой флот на погибель.");
        } else if (type == 3) {
            victoryAlert.setHeaderText("Другой игрок отключился...");
        }
        victoryAlert.setContentText("Начать новую игру или выйти?");

        ButtonType newGameButton = new ButtonType("Новая игра");
        ButtonType exitButton = new ButtonType("Выход");
        victoryAlert.getButtonTypes().setAll(newGameButton, exitButton);

        Platform.runLater(() -> {
            Optional<ButtonType> result = victoryAlert.showAndWait();
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
            clientSocket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            inputStream = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            outputStream = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

            ClientController client = new ClientController();

            serverListenerThread = new Thread(() -> {
                try {
                    while (!clientSocket.isClosed()) {
                        if (inputStream.ready()) {
                            client.readResponse();
                        }
                    }
                } catch (IOException e) {
                    System.out.println("");
                } finally {
                    System.out.printf("Game ended\n");
                }
            });
            serverListenerThread.start();

        } catch (IOException e) {
            System.err.println("Ошибка подключения: " + e.getMessage());
            e.printStackTrace();
            Platform.exit();
            throw new RuntimeException(e.getMessage());
        }

        waitingAlert.setTitle("К бою");
        waitingAlert.setHeaderText("Ждём другого игрока...");

        waitingAlert.getDialogPane().lookupButton(ButtonType.OK).setVisible(false);
        waitingAlert.show();
    }

    public void readResponse() {
        try {
            String action = inputStream.readLine();
            MessageController input = MessageController.parseFromRawMessage(action);
            switch (input.getMessageType()) {
                case HEARTBEAT:
                    outputStream.write("0#pong\n");
                    outputStream.flush();
                    break;
                case RESPONSE:
                    gameController.colorPoints(input.getGameEvents(), gameController.enemyButtons);
                    break;
                case DISCONNECT:
                    showEndingOption(3);
                    break;
                case GAME_OVER:
                    System.out.printf("%s\n", action);
                    System.out.printf("%s\n", input);
                    System.out.printf("%s\n", input.getGameEvents().toString());

                    gameController.colorPoints(input.getGameEvents(), gameController.enemyButtons);
                    showEndingOption(1);
                    break;
                case START:
                    Platform.runLater(waitingAlert::close);
                    break;
                case TURN:
                    Platform.runLater(() -> readyButton.setText("Ваш ход"));
                    gameController.prepareMove();
                    break;
                case STEP:
                    var resulting = gameController.enemyMakeStep(input.getGameEvents());
                    if (Boolean.TRUE.equals(resulting.second)) {
                        showEndingOption(2);
                        outputStream.write("5#");
                    } else {
                        outputStream.write("2#");
                    }
                    outputStream.write(GameEvent.eventsToString(resulting.first) + "\n");
                    outputStream.flush();
                    break;
            }
        } catch (IOException e) {
            System.out.printf(e.getMessage());
            e.printStackTrace();
        }
    }

    public static void startGame() {
        gameController.clearBattlefield();
        gameController.clearFields();
        readyButton.setVisible(true);
        readyButton.setDisable(false);
        readyButton.setText("Let's GO!");
    }

    private static void closeConnections() {
        try {
            if (inputStream != null) {
                inputStream.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
            if (serverListenerThread != null && serverListenerThread.isAlive()) {
                serverListenerThread.interrupt();
                serverListenerThread.join();
            }
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException | InterruptedException e) {
            System.out.printf(e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    private static void restartApplication() {
        closeConnections();
        Platform.runLater(ClientController::startGame);
    }

    public static void exitProgram() {
        try {
            if (outputStream != null) {
                outputStream.write("EXIT");
                outputStream.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeConnections();
            Platform.exit();
        }
    }

    public void makeMove(int x, int y) {
        try {
            outputStream.write(String.format("1#0,%d,%d;\n", x, y));
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}