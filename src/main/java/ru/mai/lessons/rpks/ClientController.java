package ru.mai.lessons.rpks;


import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.Pair;

import java.io.IOException;

import java.util.List;

public class ClientController {
    private int currentXCoord = 0;
    private int currentYCoord = 0;
    private String currentDirection = "right";
    private int currentShipSize = 0;
    private int shipIndex = 0;

    private final int[] shipSizes = {4, 3, 3, 2, 2, 2, 1, 1, 1, 1};
    private final BattleField battleField = new BattleField();

    @FXML
    private GridPane clientGrid, enemyGrid;

    @FXML
    private TextField textForClient, textDirection;

    @FXML
    private Button buttonStart, buttonLeft, buttonRight, buttonUp, buttonDown, buttonClear, buttonReady, buttonSetUp;

    @FXML
    private MenuItem menuItemAboutGame;

    @FXML
    private MenuItem menuItemRules;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private MenuBar menuBar;


    ClientHandler clientHandler;

    private Timeline countdownTimeline;




    public void setStage(Stage stage) {

        stage.setOnCloseRequest(event -> {
            if (clientHandler != null) {
                sendMessage("exit");
                clientHandler.downService();
            }
            Platform.exit();

        });
    }

    private void initializeClient() throws IOException {
        try {
            clientHandler = new ClientHandler("localhost", 8080, this);
        } catch (IOException e) {
            textForClient.setText("Упс, сервер лежит, попробуйте позже!");
            throw new IOException(e.getMessage());
        }
    }

    public void sendMessage(String message) {
        if (message != null) {
            clientHandler.addMessage(message);
        }
    }

    public void receiveMessage(String message) {
        Platform.runLater(() -> {
            try {
                if (message.startsWith("shot:")) {
                    String[] parts = message.split(":")[1].split(",");
                    int x = Integer.parseInt(parts[0]);
                    int y = Integer.parseInt(parts[1]);
                    battleField.shootAtCoordinates(x, y);
                    updateGrid();

                    if (battleField.isHit(x, y)) {
                        enemyGrid.setDisable(true);
                        sendMessage("hurt:" + x + "," + y);
                        if (battleField.isShipSunk(x, y)) {
                            List<Pair<Integer, Integer>> listYellow = battleField.getYellowArea(x,y);

                            StringBuilder messageYellowArea = new StringBuilder();

                            for (Pair<Integer, Integer> pair : listYellow) {
                                messageYellowArea.append(pair.getKey().toString()).append(",").append(pair.getValue()).append(",");
                            }

                            messageYellowArea.deleteCharAt(messageYellowArea.length() - 1);

                            sendMessage("kill:" + messageYellowArea);
                        }
                        if (!battleField.areShipsAlive()) {
                            sendMessage("win");
                            resetGame();
                            textForClient.setText("Вы проиграли, можете попробовать снова, нажав Start");
                            buttonSetUp.setDisable(true);
                            clientGrid.setDisable(true);
                        }
                    } else {
                        sendMessage("miss:" + x + "," + y);
                        textForClient.setText("Противник промахнулся, ход за вами");
                        enemyGrid.setDisable(false);
                        progressBar.setVisible(true);
                        startCountdown(19);
                    }
                } else if (message.startsWith("hurt:")) {
                    String[] parts = message.split(":")[1].split(",");
                    int x = Integer.parseInt(parts[0]);
                    int y = Integer.parseInt(parts[1]);
                    if (x != -1 && y != -1) {
                        hurtHandler(x, y);
                        textForClient.setText("Отлично, вы попали, продолжайте стрелять!");
                        progressBar.setVisible(true);
                        startCountdown(19);
                    } else {
                        textForClient.setText("Начинайте, ход за вами.");
                        progressBar.setVisible(true);
                        startCountdown(19);
                    }
                    enemyGrid.setDisable(false);
                } else if (message.startsWith("miss:")) {
                    String[] parts = message.split(":")[1].split(",");
                    int x = Integer.parseInt(parts[0]);
                    int y = Integer.parseInt(parts[1]);
                    if (x != -1 && y != -1) {
                        missHandler(x, y);
                        textForClient.setText("Ход противника.");
                        progressBar.setVisible(false);
                    } else {
                        textForClient.setText("Ход противника");
                        progressBar.setVisible(false);
                    }
                    enemyGrid.setDisable(true);
                } else if (message.startsWith("kill:")) {
                    textForClient.setText("Отлично, вы потопили корабль противника. Ход за вами");
                    String[] parts = message.split(":")[1].split(",");
                    for (int i = 0; i < parts.length; i += 2) {
                        int x = Integer.parseInt(parts[i]);
                        int y = Integer.parseInt(parts[i + 1]);
                        missHandler(x, y);
                    }
                    progressBar.setVisible(true);
                    startCountdown(19);
                } else if (message.startsWith("interrupt")) {
                    resetGame();
                    progressBar.setVisible(false);
                } else if (message.startsWith("closed")) {
                    buttonReady.setDisable(false);
                    textForClient.setText("Пока на сервере максимальное число игроков, попробуйте подключиться позже");
                    if (clientHandler != null) {
                        clientHandler.downService();
                        clientHandler = null;
                    }
                } else if (message.startsWith("win")) {
                    resetGame();
                    buttonSetUp.setDisable(true);
                    textForClient.setText("Вы победили! Поздравляем, нажмите Start, если хотите начать заново");
                    progressBar.setVisible(false);
                    countdownTimeline.stop();
                    //clientGrid.setDisable();

                } else if (message.startsWith("to")) {
                    resetGame();
                    textForClient.setText("Превышение времени ожидания! Нажми Start для новой игры");
                    progressBar.setVisible(false);
                    buttonSetUp.setDisable(true);
                    menuBar.setDisable(false);
                } else if (message.startsWith("exit")) {
                    resetGame();
                    textForClient.setText("Вам засчитана автоматическая победа, так как противник отключился");
                    progressBar.setVisible(false);
                    buttonSetUp.setDisable(true);
                    clientGrid.setDisable(true);
                } else if (message.startsWith("depth")) {
                    resetGame();
                    textForClient.setText("Упс, сервер упал во время вашей игры");
                    progressBar.setVisible(false);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void resetGame() {
        battleField.clearGrid();
        updateGrid();
        currentXCoord = 0;
        currentYCoord = 0;
        currentDirection = "right";
        currentShipSize = 0;
        shipIndex = 0;

        textDirection.setText("Направление: вправо");
        muteButtons(false);
        buttonStart.setDisable(false);
        buttonReady.setDisable(true);
        buttonSetUp.setDisable(true);
        clientGrid.setDisable(true);
        enemyGrid.getChildren().clear();
        enemyGrid.setGridLinesVisible(false);


        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                Button button = new Button();

                button.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

                final int row = i;
                final int col = j;

                button.setOnAction(event -> {
                    button.setDisable(true);
                    handleCellClickEnemy(row, col);
                });

                enemyGrid.add(button, j, i);
            }
        }

        enemyGrid.setDisable(true);

        if (clientHandler != null) {
            clientHandler.downService();
            clientHandler = null;
        }
    }

    public void missHandler(int x, int y) {
        Button buttonOfEnemyArea = getButtonFromGrid(x,y,enemyGrid);
        if (buttonOfEnemyArea != null) {
            buttonOfEnemyArea.setStyle("-fx-background-color: #f8e606;");
            buttonOfEnemyArea.setDisable(true);
        }
    }

    public void hurtHandler(int x, int y) {
        Button buttonOfEnemyArea = getButtonFromGrid(x,y,enemyGrid);
        if (buttonOfEnemyArea != null) {
            buttonOfEnemyArea.setStyle("-fx-background-color: #f40000;");

        }

    }

    public void initialize() {
        setUpGrids();
        textDirection.setEditable(false);
        textForClient.setEditable(false);
        textForClient.setText("Добро пожаловать! Нажмите 'Start' для начала.");
        textDirection.setText("Направление: вправо");
        muteButtons(false);

        menuItemAboutGame.setOnAction(event -> showAlert("About the Game", "Морской бой — это пошаговая стратегическая игра, " +
                "где игроки пытаются потопить корабли друг друга. Здесь важно все: расстановка ваших кораблей, расстановка кораблей противника " +
                "и красавица-удача, конечно) Побеждайте в морских боях!"));

        menuItemRules.setOnAction(event -> showAlert("Game Rules", """
                1.Нажмите Start.
                2.Вам будет предложен корабль определенной палубности.
                3. Выберите одно из направлений постановки корабля.
                4. Расставьте все свои корабли - Учтите, что корабли не могут пересекаться и в радиусе каждого корабля должна быть хотя бы одна свободная клетка.
                5. Нажмите Ready и дожидайтесь подключения противника.
                6. Стреляйте по координатам, чтобы потопить корабли противника.
                7. Побеждает тот, кто первым уничтожит весь флот.
                8. !!!Учтите, что нажимая кнопку SetUp вы окончательно определились со своей расстановкой кораблей!!!"""));

        progressBar.setVisible(false);
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public void clickOnStart() {
        muteButtons(true);
        buttonReady.setDisable(true);
        enemyGrid.setDisable(true);
        shipIndex = 0;
        buttonSetUp.setDisable(true);
        placeNextShip();
        menuBar.setDisable(false);
    }

    private void placeNextShip() {
        if (shipIndex >= shipSizes.length) {
            textForClient.setText("Все корабли успешно размещены! Нажмите SetUp");
            muteButtons(false);
            buttonClear.setDisable(false);
            buttonStart.setDisable(true);
            clientGrid.setDisable(true);
            buttonSetUp.setDisable(false);
            buttonReady.setDisable(true);
            return;
        }

        currentShipSize = shipSizes[shipIndex];
        textForClient.setText("Разместите корабль на " + currentShipSize + " палубы.");
        if (currentShipSize == 1) {
            currentDirection = "none";
        }
    }



    public void handleCellClick(int x, int y) {
        currentXCoord = x;
        currentYCoord = y;
        System.out.println(x + " " + y);

        Ship ship = new Ship(currentShipSize);
        if (battleField.canPlaceShip(ship, currentXCoord, currentYCoord, currentDirection)) {
            battleField.placeShip(ship, currentXCoord, currentYCoord, currentDirection);
            shipIndex++;
            updateGrid();
            placeNextShip();

        } else {
            textForClient.setText("Неверное размещение! Попробуйте снова.");
        }
    }


    public void handleCellClickEnemy(int x, int y) {
        System.out.println(x + " " + y);
        sendMessage("shot:" + x + "," + y);
        countdownTimeline.stop();
    }

    private void updateGrid() {
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                Button button = getButtonFromGrid(i, j, clientGrid);
                if (battleField.getGrid()[i][j] == 'O') {
                    if (button != null) {
                        button.setStyle("-fx-background-color: black;");
                    }
                }
                if (battleField.getGrid()[i][j] == '.') {
                    if (button != null) {
                        button.setStyle("");
                    }
                }
                if (battleField.getGrid()[i][j] == 'X') {
                    if (button != null) {
                        button.setStyle("-fx-background-color: #f40000;");
                    }
                }
                if (battleField.getGrid()[i][j] == '-') {
                    if (button != null) {
                        button.setStyle("-fx-background-color: #f8e606;");
                    }
                }
            }
        }
    }

    private Button getButtonFromGrid(int row, int column, GridPane grid) {
        for (Node node : grid.getChildren()) {
            Integer rowIndex = GridPane.getRowIndex(node);
            Integer colIndex = GridPane.getColumnIndex(node);

            if (rowIndex != null && colIndex != null && rowIndex == row && colIndex == column && node instanceof Button) {
                return (Button) node;
            }
        }
        return null;
    }

    private void startCountdown(int seconds) {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }

        progressBar.setProgress(1.0);

        countdownTimeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(progressBar.progressProperty(), 1.0)),
                new KeyFrame(Duration.seconds(seconds), new KeyValue(progressBar.progressProperty(), 0.0))
        );

        countdownTimeline.play();
    }


    public void setUpGrids() {
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                Button button1 = new Button();
                Button button2 = new Button();
                button1.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                button2.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

                final int row = i;
                final int col = j;

                button1.setOnAction(event -> handleCellClick(row, col));
                button2.setOnAction(event -> {
                    button2.setDisable(true);
                    handleCellClickEnemy(row, col);
                });
                clientGrid.add(button1, j, i);
                enemyGrid.add(button2, j, i);
            }
        }
    }



    public void clickOnLeft() {
        currentDirection = "left";
        textDirection.setText("Направление: влево.");
    }

    public void clickOnRight() {
        currentDirection = "right";
        textDirection.setText("Направление: вправо.");
    }

    public void clickOnUp() {
        currentDirection = "up";
        textDirection.setText("Направление: вверх.");
    }

    public void clickOnDown() {
        currentDirection = "down";
        textDirection.setText("Направление: вниз.");
    }

    public void clearClient() {
        battleField.clearGrid();

        currentDirection = "right";
        textDirection.setText("right");
        updateGrid();
        clickOnStart();
    }


    private void muteButtons(boolean isMute) {
        buttonStart.setDisable(isMute);
        buttonLeft.setDisable(!isMute);
        buttonRight.setDisable(!isMute);
        buttonUp.setDisable(!isMute);
        buttonDown.setDisable(!isMute);
        buttonClear.setDisable(!isMute);
        clientGrid.setDisable(!isMute);
        enemyGrid.setDisable(!isMute);
        buttonReady.setDisable(!isMute);
        buttonSetUp.setDisable(!isMute);
    }

    public void clickOnSetUp() {
        buttonSetUp.setDisable(true);
        buttonClear.setDisable(true);
        buttonReady.setDisable(false);
    }

    public void clickOnReady() {
        try {
            initializeClient();
            buttonReady.setDisable(true);
            enemyGrid.setDisable(true);
            sendMessage("ready");
            progressBar.setVisible(true);
            startCountdown(14);
            menuBar.setDisable(true);
        } catch (IOException e) {
            buttonReady.setDisable(false);
        }

    }
}
