package ru.mai.lessons.rpks.Client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;
import ru.mai.lessons.rpks.controllers.GameController;
import ru.mai.lessons.rpks.controllers.MessageController;
import ru.mai.lessons.rpks.include.GameEvent;

import java.io.*;
import java.net.Socket;
import java.util.*;

public final class ClientController extends Application {

    private static Player player;
    private InputStream inputStreamMusic;
    private static boolean isMusicPlaying = false;
    private Button musicButton;

    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int SERVER_PORT = 8081;
    private static Socket clientSocket;

    private static BufferedReader inputStream;
    private static BufferedWriter outputStream
            ;
    private static Alert waitingAlert;
    private static Alert victoryAlert;

    private static final GameController gameController = new GameController();
    private static Thread serverListenerThread;
    private static Button readyButton;
    private static Button clearButton;

    private boolean isVictoryAlertActive = false;

    @Override
    public void start(Stage stage) {
        BorderPane rootLayout = createRootLayout();

        Image backgroundImage = new Image(Objects.requireNonNull(getClass().getResource("/images/background.jpg")).toExternalForm());
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
        mainScene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("styles.css")).toExternalForm());

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

    private void playBackgroundMusic() {
        try {
            String path = "/sounds/shipbattle.mp3";
            inputStreamMusic = getClass().getResourceAsStream(path);

            if (inputStreamMusic == null) {
                throw new IOException("Music file not found");
            }

            player = new Player(inputStreamMusic);

            new Thread(() -> {
                try {
                    while (isMusicPlaying) {
                        player.play();
                        inputStreamMusic = getClass().getResourceAsStream(path);
                        if (inputStreamMusic != null) {
                            player = new Player(inputStreamMusic);
                        } else {
                            break;
                        }
                    }
                } catch (JavaLayerException e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (JavaLayerException | IOException e) {
            e.printStackTrace();
        }
    }


    private void toggleMusic() {
        try {
            if (isMusicPlaying) {
                isMusicPlaying = false;
                if (player != null) {
                    player.close();
                }
                if (inputStreamMusic != null) {
                    inputStreamMusic.close();
                }
                musicButton.setText("Play Music");
            } else {
                isMusicPlaying = true;
                playBackgroundMusic();
                musicButton.setText("Stop Music");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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

        HBox topBar = new HBox(10);
        Button menuBar = createHelpMenu();
        musicButton = new Button("Play Music");
        musicButton.setOnAction(e -> toggleMusic());

        clearButton = new Button("Clear Battlefield");
        clearButton.setOnAction(e -> gameController.clearFields());

        topBar.getChildren().addAll(musicButton, menuBar, clearButton);
        topBar.setAlignment(Pos.CENTER_LEFT);
        root.setTop(topBar);

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
        Label enemyLabel = createLabel("Enemy Grid");
        enemyLabel.setStyle(
                "-fx-text-fill: red;" +
                        "-fx-font-family: 'Arial';" +
                        "-fx-font-size: 20px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-effect: dropshadow(gaussian, black, 3, 0.5, 0, 1);");
        GridPane enemyGrid = createEnemyGrid();

        VBox enemyField = new VBox(10, enemyLabel, enemyGrid);
        enemyField.setAlignment(Pos.CENTER);

        return enemyField;
    }

    private VBox createPlayerField() {
        Label playerLabel = createLabel("Your Grid");
        playerLabel.setStyle(
                "-fx-text-fill: red;" +
                        "-fx-font-family: 'Arial';" +
                        "-fx-font-size: 20px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-effect: dropshadow(gaussian, black, 3, 0.5, 0, 1);");
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
            Button menuButton = new Button("Menu");
            menuButton.setOnAction(e -> showLanguageChoiceDialog());
            return menuButton;
        }

    private void showLanguageChoiceDialog() {
        List<String> languages = Arrays.asList("Русский", "English");

        ChoiceDialog<String> dialog = new ChoiceDialog<>("Русский", languages);
        dialog.setTitle("Выбор языка");
        dialog.setHeaderText("Выберите язык");
        dialog.setContentText("Язык:");

        dialog.showAndWait().ifPresent(this::showRulesAndInfo);
    }

    private void showRulesAndInfo(String language) {
        Stage rulesStage = new Stage();
        rulesStage.setTitle("Rules and Information");

        TextArea textArea = getTextArea(language);
        textArea.setWrapText(true);
        textArea.setEditable(false);

        VBox layout = new VBox();
        VBox.setVgrow(textArea, Priority.ALWAYS);
        layout.getChildren().add(textArea);
        layout.setPadding(new Insets(10));

        Scene scene = new Scene(layout, 500, 500);
        rulesStage.setScene(scene);

        rulesStage.initModality(Modality.APPLICATION_MODAL);

        rulesStage.showAndWait();
    }

    private static TextArea getTextArea(String language) {
        String rulesText;
        if ("English".equals(language)) {
            rulesText = """
                Rules:
                
                1. Place your ships on the grid.
                2. Take turns attacking the opponent's grid by selecting cells.
                3. The first player to sink all opponent's ships wins.
                
                Place ship - LMB
                Remove ship - RMB
                
                About the Game:
                Battleship is a classic two-player strategy game. Originally played on paper,
                it has evolved into a beloved board game and digital adaptation.
                Players use logic and strategy to locate and destroy enemy ships.
                
                Have fun and good luck!
                """;
        } else {
            rulesText = """
                Правила:
                
                Разместите свои корабли на сетке.
                Поочередно атакуйте сетку противника, выбирая клетки.
                Первый игрок, потопивший все корабли противника, побеждает.
                Разместить корабль — ЛКМ
                Удалить корабль — ПКМ
                
                Об игре:
                "Морской бой" — это классическая стратегическая игра для двух игроков.
                Изначально она игралась на бумаге, но со временем превратилась в популярную настольную и цифровую версию.
                Игроки используют логику и стратегию, чтобы обнаружить и уничтожить корабли противника.
                
                Приятной игры и удачи!
                """;
        }
        return new TextArea(rulesText);
    }

    private GridPane createPlayerGrid() {
        GridPane playerGrid = new GridPane();
        String imageUrl = Objects.requireNonNull(getClass().getResource("/images/sea.jpg")).toExternalForm();
        System.out.println("Image URL: " + imageUrl);
        for (int row = 0; row < 10; row++) {
            List<Button> buttons = new ArrayList<>();

            for (int col = 0; col < 10; col++) {
                Button cellButton = new Button();
                cellButton.setMinSize(40, 40);

                int finalRow = row;
                int finalCol = col;

                cellButton.setOnMouseClicked(event -> handlePlayerGridClick(finalRow, finalCol, cellButton, event));

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
                cellButton.setMinSize(40, 40);
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
            readyButton.setText("Waiting...The enemy is thinking");
            gameController.endMove();
        }
    }

    private void start() {
        if (!gameController.checkField()) {
            showWarningWrongShips();
        } else {
            readyButton.setDisable(true);
            readyButton.setVisible(false);
            waitingServer();
            clearButton.setDisable(true);
            clearButton.setVisible(false);
        }
    }

    private void showWarningWrongShips() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Error");
        alert.setHeaderText("Invalid Number of Ships");
        alert.setContentText("""
            Your fleet configuration is incorrect.
            Please make sure you have the following ships:
            
            - 1 ship of size 4 (4 cells)
            - 2 ships of size 3 (3 cells each)
            - 3 ships of size 2 (2 cells each)
            - 4 ships of size 1 (1 cell each)
            
            Make sure all ships fit within the grid without overlapping or exceeding boundaries.
            """);
        alert.showAndWait();
    }


    private void showEndingOption(int type) {
        if (isVictoryAlertActive) {
            return;
        }
        isVictoryAlertActive = true;

        victoryAlert.setTitle("Game Over");
        victoryAlert.setHeaderText(null);
        String backgroundImage = switch (type) {
            case 1 -> {
                victoryAlert.setHeaderText("Congratulations!");
                yield "/images/victory_background.jpg";
            }
            case 2 -> {
                victoryAlert.setHeaderText("Alas... You lost!");
                yield "/images/defeat_background.png";
            }
            case 3 -> {
                victoryAlert.setHeaderText("The other player disconnected...");
                yield "/images/disconnect_background.jpg";
            }
            default -> {
                victoryAlert.setHeaderText("Unknown game status.");
                yield "/images/disconnect_background.jpg";
            }
        };

        victoryAlert.setContentText("Start a new game or exit?");

        ButtonType newGameButton = new ButtonType("New Game");
        ButtonType exitButton = new ButtonType("Exit");
        victoryAlert.getButtonTypes().setAll(newGameButton, exitButton);

        DialogPane dialogPane = victoryAlert.getDialogPane();
        dialogPane.setStyle(
                "-fx-background-image: url('" + getClass().getResource(backgroundImage).toExternalForm() + "');" +
                        "-fx-background-size: cover;" +
                        "-fx-background-position: center;" +
                        "-fx-pref-width: 600px;" +
                        "-fx-pref-height: 400px;" +
                        "-fx-padding: 20px;"
        );

        Label headerLabel = new Label(victoryAlert.getHeaderText());
        headerLabel.setStyle(
                "-fx-font-size: 24px;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;"
        );
        dialogPane.setHeader(headerLabel);

        Label contentLabel = new Label(victoryAlert.getContentText());
        contentLabel.setStyle(
                "-fx-font-size: 18px;" +
                        "-fx-text-fill: white;"
        );
        victoryAlert.setContentText(null);
        dialogPane.setContent(contentLabel);

        Platform.runLater(() -> {
            Optional<ButtonType> result = victoryAlert.showAndWait();
            result.ifPresent(buttonType -> {
                if (buttonType == newGameButton) {
                    victoryAlert.close();
                    restartApplication();
                } else {
                    victoryAlert.close();
                    exitProgram();
                }
            });
            isVictoryAlertActive = false;
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
                } catch (IOException ignored) {
                } finally {
                    System.out.println("Quit");
                }
            });
            serverListenerThread.start();

        } catch (IOException e) {
            System.err.println("Connection error: " + e.getMessage());
            e.printStackTrace();
            Platform.exit();
            throw new RuntimeException(e.getMessage());
        }

        waitingAlert.setTitle("Prepare for Battle");
        waitingAlert.setHeaderText(null);
        waitingAlert.setGraphic(null);

        String gifPath = getClass().getResource("/images/loading_animation.gif").toExternalForm();
        VBox contentBox = getVBox(gifPath);

        waitingAlert.getDialogPane().setStyle("-fx-background-color: black;");

        waitingAlert.getDialogPane().setContent(contentBox);

        waitingAlert.getDialogPane().lookupButton(ButtonType.OK).setVisible(false);

        waitingAlert.show();
    }


    private static VBox getVBox(String gifPath) {
        ImageView gifView = new ImageView(new Image(gifPath));
        gifView.setFitWidth(400);
        gifView.setFitHeight(200);
        gifView.setPreserveRatio(true);

        Label waitingLabel = new Label("Waiting for another player...");
        waitingLabel.setStyle(
                "-fx-font-size: 18px;" +
                        "-fx-font-family: 'Arial';" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;"
        );

        VBox contentBox = new VBox(10, gifView, waitingLabel);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setStyle("-fx-background-color: black;");
        return contentBox;
    }


    public void readResponse() {
        try {
            String action = inputStream.readLine();
            MessageController input = MessageController.parseFromRawMessage(action);
            switch (input.getMessageType()) {
                case HEARTBEAT:
                    outputStream.write("0#\n");
                    outputStream.flush();
                    break;
                case RESPONSE:
                    gameController.colorPoints(input.getGameEvents(), gameController.enemyButtons);
                    break;
                case DISCONNECT:
                    showEndingOption(3);
                    break;
                case GAME_OVER:
                    gameController.colorPoints(input.getGameEvents(), gameController.enemyButtons);
                    showEndingOption(1);
                    break;
                case START:
                    Platform.runLater(waitingAlert::close);
                    break;
                case TURN:
                    Platform.runLater(() -> readyButton.setText("Your turn"));
                    gameController.prepareMove();
                    break;
                case STEP:
                    var resulting = gameController.enemyMakeStep(input.getGameEvents());
                    if (Boolean.TRUE.equals(resulting.second())) {
                        showEndingOption(2);
                        outputStream.write("5#");
                    } else {
                        outputStream.write("2#");
                    }
                    outputStream.write(GameEvent.eventsToString(resulting.first()) + "\n");
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
        clearButton.setVisible(true);
        clearButton.setDisable(false);
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
            if (isMusicPlaying) {
                isMusicPlaying = false;
                if (player != null) {
                    player.close();
                }
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