package ru.mai.lessons.rpks.Game;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import ru.mai.lessons.rpks.Client.ClientThread;
import ru.mai.lessons.rpks.Game.Field.Field;
import ru.mai.lessons.rpks.Game.Logic.Battle;
import ru.mai.lessons.rpks.Game.Logic.ShipPlacement;

import java.io.*;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Game extends Application {
    private static final Logger logger = Logger.getLogger(Game.class.getName());

    public static Field field;
    public static Field opponentField;

    public static GridPane playerGrid;
    public static GridPane opponentGrid;

    public static Text text;
    public static Button finishButton;
    public static Button clearButton;

    private static Stage stage;

    public static ClientThread clientThread;

    public static Text turnText = new Text();

    private static MenuBar menuBar;

    public static final BooleanProperty isConnectedProperty = new SimpleBooleanProperty(false);

    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 5000;

    public static int wins = 0;
    public static boolean isInBattle = false;
    public static boolean isInShipPlacement = false;

    public static String SHIP_STYLE = "-fx-background-color: lightblue;";
    public static String SHOT_SHIP_STYLE = "-fx-background-color: red;";
    public static String MISS_STYLE = "-fx-background-color: gray;";

    public static String BASIC_TEXT_STYLE = "-fx-font-size: 16;";
    public static String SMALL_TEXT_STYLE = "-fx-font-size: 12;";
    public static String BIG_TEXT_STYLE = "-fx-font-size: 18;";
    public static String VERY_BIG_TEXT_STYLE = "-fx-font-size: 18;";

    public static String BUTTON_STYLE = BASIC_TEXT_STYLE + " -fx-padding: 5;";
    public static String MENU_STYLE = SMALL_TEXT_STYLE + " -fx-padding: 5;";

    public static String SHOT = "SHOT";
    public static String MISS = "MISS";
    public static String SHOT_RESULT = "SHOT_RESULT";
    public static String CLOSE = "CLOSE";
    public static String STOP = "STOP";
    public static String OPPONENT_READY = "OPPONENT READY";
    public static String READY = "READY";
    public static String TURN = "TURN";
    public static String NEW_GAME = "NEW GAME";

    public static void run() {
        launch();
    }

    public static void addClientThread(Socket socket) throws IOException {
        Game.clientThread = new ClientThread(socket);
        clientThread.start();
        isConnectedProperty.set(true);
    }

    public static void removeClientThread() {
        clientThread.stopThread();
        clientThread.interrupt();
        Game.clientThread = null;
        isConnectedProperty.set(false);
    }

    @Override
    public void start(Stage stage) {
        Game.stage = stage;
        stage.setResizable(false);

        stage.setOnCloseRequest(event -> {
            if (clientThread != null) {
                try {
                    clientThread.sendMessage("STOP");
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Unable to send message", e);
                }
                clientThread.stopThread();
            }
            Platform.runLater(() -> {
                Platform.exit();
                System.exit(0);
            });
        });

        isConnectedProperty.addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                Platform.runLater(Game::startShipPlacement);
            }
        });

        createMenu();
        showStartingScreen();
    }

    public static void showStartingScreen() {
        isInShipPlacement = false;
        isInBattle = false;
        field = new Field();
        opponentField = new Field();

        Button newGameButton = new Button("Начать игру");
        newGameButton.setOnAction(e -> {
            if (!isConnectedProperty.get()) {
                Platform.runLater(Game::startConnectionToServer);
            } else {
                Platform.runLater(Game::startShipPlacement);
            }
        });
        newGameButton.setStyle(BUTTON_STYLE);

        Text text = new Text("""
                Морской бой — это стратегическая настольная игра
                для двух игроков, сочетающая элементы тактики и удачи.
                Цель игры — первым уничтожить все корабли противника,
                угадав их расположение.
                """);
        text.setTextAlignment(TextAlignment.CENTER);
        text.setStyle(BASIC_TEXT_STYLE);

        Text winsText = new Text("Победы: " + wins);
        winsText.setTextAlignment(TextAlignment.CENTER);
        winsText.setStyle(BASIC_TEXT_STYLE);

        BorderPane container = new BorderPane();
        container.setTop(text);
        container.setBottom(winsText);
        container.setCenter(newGameButton);
        container.setPadding(new Insets(20));
        BorderPane root = new BorderPane();
        root.setCenter(container);
        root.setTop(menuBar);

        stage.setScene(new Scene(root));
        stage.sizeToScene();
        stage.setTitle("Морской бой");
        stage.show();
    }

    public static void startConnectionToServer() {
        new Thread(Game::connect).start();

        Text text = new Text("Ожидание подключения к серверу...");
        text.setStyle(BIG_TEXT_STYLE);
        StackPane stackPane = new StackPane(text);
        BorderPane root = new BorderPane();
        root.setTop(menuBar);
        root.setCenter(stackPane);
        stage.setScene(new Scene(root, 500, 200));
        stage.setTitle("Ожидание подключения к серверу");
        stage.show();
    }

    private static void connect() {
        int retryCount = 0;
        int maxRetries = 10;
        while (!isConnectedProperty.get() && retryCount < maxRetries) {
            logger.log(Level.INFO, "Trying to connect to server...");
            try {
                Thread.sleep(2000);
                Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                Game.addClientThread(socket);
                logger.log(Level.INFO, "Connected to server");
                return;
            } catch (IOException e) {
                logger.log(Level.INFO, "Can't connect to server.. trying again");
            } catch (InterruptedException e) {
                logger.log(Level.SEVERE, "Thread was interrupted", e);
                Thread.currentThread().interrupt();
                Platform.exit();
                System.exit(0);
            }
            ++retryCount;
        }
        if (retryCount == maxRetries) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Ошибка подключения");
                alert.setHeaderText("Не удалось подключиться к серверу");
                alert.setContentText("Попробуйте позже.");
                alert.showAndWait().ifPresent(e -> Game.showStartingScreen());
            });
        }
    }

    private void createMenu() {
        MenuItem rules = new MenuItem("Правила");
        rules.setOnAction(e -> showRules());
        MenuItem information = new MenuItem("Об игре");
        information.setOnAction(e -> showInformationAboutGame());
        Menu menu = new Menu("Меню");
        menu.setStyle(MENU_STYLE);
        menu.getItems().addAll(rules, information);
        menuBar = new MenuBar(menu);
    }

    private void showRules() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Правила");
        alert.setHeaderText("Правила игры в морской бой");
        alert.setContentText("""
                1. Каждый игрок размещает на своём поле корабли: один размером 4 клетки,
                два — по 3 клетки, три — по 2 клетки и четыре — по 1 клетке.
                Корабли не могут перекрывать друг друга и должны быть расположены горизонтально или вертикально.
                
                2. Игроки по очереди нажимают на клетки,
                в которых, по их мнению, находится корабль противника.
                
                3. Если выстрел попал в корабль, противник сообщает о попадании. Если нет — о промахе.
                
                4. Цель игры — уничтожить все корабли противника, угадав их расположение.
                
                5. Игра заканчивается, когда один из игроков потопит все корабли другого.""");
        alert.showAndWait();
    }

    private void showInformationAboutGame() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Об игре");
        alert.setHeaderText("Что такое «Морской бой»?");
        alert.setContentText("""
                Морской бой — это стратегическая настольная игра для двух игроков.
                Каждый игрок размещает свои корабли на поле размером 10x10 клеток,
                при этом корабли могут занимать несколько клеток подряд (от одной до четырех).
                Игроки поочередно называют координаты, где, по их мнению, могут быть корабли противника.
                Если в выбранной клетке оказывается корабль, это считается попаданием, если нет — промахом.
                
                Цель игры — первым уничтожить все корабли соперника.
                Игра продолжается, пока один из игроков не потопит все корабли противника.""");
        alert.showAndWait();
    }

    public static void startShipPlacement() {
        isInShipPlacement = true;
        text = new Text(ShipPlacement.getShipsInfo(field));
        text.setStyle(BIG_TEXT_STYLE);
        text.setTextAlignment(TextAlignment.CENTER);

        playerGrid = ShipPlacement.getGridPane(field, text);

        setFinishButton();

        clearButton = new Button("Очистить поле");
        clearButton.setStyle(MENU_STYLE);
        clearButton.setOnAction(event -> clear());

        VBox vBox = new VBox(text, clearButton, finishButton);
        vBox.setSpacing(20);
        vBox.setAlignment(Pos.CENTER);

        HBox container = new HBox(menuBar, playerGrid, vBox);
        container.setSpacing(10);
        container.setPadding(new Insets(20));

        BorderPane root = new BorderPane();
        root.setTop(menuBar);
        root.setCenter(container);

        stage.setScene(new Scene(root));
        stage.setTitle("Расстановка кораблей");
        stage.sizeToScene();
        stage.show();
    }

    private static void setFinishButton() {
        finishButton = new Button("Завершить");
        finishButton.setOnAction(event -> {
            if (!field.isFilled()) {
                logger.log(Level.WARNING, "Некорректная расстановка");
                clear();
            } else {
                try {
                    clientThread.sendMessage(READY);
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Unable to send message", e);
                }
                startWaitingScreen();
            }
        });
        finishButton.setStyle(MENU_STYLE);
    }

    private static void clear() {
        field = new Field();
        startShipPlacement();
    }

    public static void startWaitingScreen() {
        isInShipPlacement = false;
        Text text = new Text("Ожидание противника...");
        text.setStyle(VERY_BIG_TEXT_STYLE);
        StackPane stackPane = new StackPane(text);
        BorderPane root = new BorderPane();
        root.setTop(menuBar);
        root.setCenter(stackPane);
        stage.setScene(new Scene(root, 500, 200));
        stage.setTitle("Ожидание противника");
        stage.show();
    }

    public static void startBattle() {
        isInShipPlacement = false;
        isInBattle = true;
        playerGrid = Battle.getFieldGridPane();
        opponentGrid = Battle.getOpponentGridPane();

        HBox hBox = new HBox(playerGrid, opponentGrid);
        hBox.setSpacing(10);

        turnText.setStyle(VERY_BIG_TEXT_STYLE);
        updateTurnText();

        VBox container = new VBox(turnText, hBox);
        container.setAlignment(Pos.CENTER);
        container.setSpacing(10);
        container.setPadding(new Insets(20));

        BorderPane root = new BorderPane();
        root.setTop(menuBar);
        root.setCenter(container);

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Битва");
        stage.sizeToScene();
        stage.show();
    }

    public static void updateTurnText() {
        if (clientThread.isTurn) {
            turnText.setText("Твой ход");
        } else {
            turnText.setText("Ход противника");
        }
    }

    public static void setShotOnOpponentField(String message) {
        String[] msg = message.split(" ");
        int x = Integer.parseInt(msg[1]);
        int y = Integer.parseInt(msg[2]);
        String result = msg[3];
        if (result.equals(SHOT)) {
            Battle.setShotOnOpponentField(x, y);
        } else {
            Battle.setMissOnOpponentField(x, y);
        }
    }

    public static void setShotOnField(String message) throws IOException {
        String[] msg = message.split(" ");
        int x = Integer.parseInt(msg[1]);
        int y = Integer.parseInt(msg[2]);
        String result;
        if (field.isShipAt(x, y)) {
            Battle.setShotOnField(x, y);
            result = SHOT;
        } else {
            Battle.setMissOnField(x, y);
            result = MISS;
        }
        clientThread.sendMessage(SHOT_RESULT + " " + x + " " + y + " " + result);
    }

    public static boolean playerHasShips() {
        return field.hasRemainingShips();
    }

    public static boolean opponentHasShips() {
        return opponentField.hasRemainingShips();
    }
}
