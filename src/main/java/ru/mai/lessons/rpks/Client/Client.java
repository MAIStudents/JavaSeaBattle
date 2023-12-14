package ru.mai.lessons.rpks.Client;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.log4j.Logger;
import ru.mai.lessons.rpks.Components.InitPoints;
import ru.mai.lessons.rpks.MainController;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static ru.mai.lessons.rpks.Components.Responses.*;


public class Client {

    private static final Logger logger = Logger.getLogger(Client.class.getName());

    private String host = "127.0.0.1";
    private Integer port = 8843;
    private String name = "Bot";
    private MainController controller;


    enum gameState {
        Turn,
        Response,
        End
    }

    enum responseState {
        Win,
        Lose,
        Past,
        Wounded,
        Killed,
        Opponent_left,
        Opponent_turn
    }

    private gameState currentGameState;
    private responseState serverResponse;
    private boolean isOnline;
    private int turnX;
    private int turnY;
    private boolean isEnd;

    private final int MIN_CELL = 0;
    private final int MAX_CELL = 10;

    private BlockingQueue<gameState> gameStateQueue;
    private BlockingQueue<responseState> responseStateQueue;


    public Client() {
    }

    public Client(String host, Integer port, String name) {
        this.host = host;
        this.port = port;
        this.name = name;
    }


    public void start() {

        try {
            launch();
        } catch (IOException ignored) {
        }

        gameStateQueue = new ArrayBlockingQueue<>(5);
        responseStateQueue = new ArrayBlockingQueue<>(5);

        try {

            logger.info("Клиент инициализиролван");
            Socket server = new Socket(host, port);
            isOnline = true;
            isEnd = false;


            PrintWriter outputStream = new PrintWriter(server.getOutputStream());
            outputStream.println("New player ###" + name);
            outputStream.flush();


            new Thread(() -> {
                Scanner inputStream = null;
                try {
                    inputStream = new Scanner(server.getInputStream());

                    while (inputStream.hasNext() && !isEnd) {

                        String text = inputStream.nextLine();

                        logger.info("Ответ " + text);

                        if (text.equals(TURN)) {
                            gameStateQueue.put(gameState.Turn);
                        } else if (text.equals(WOUNDED)) {
                            gameStateQueue.put(gameState.Response);
                            responseStateQueue.put(responseState.Wounded);
                        } else if (text.equals(PAST)) {
                            gameStateQueue.put(gameState.Response);
                            responseStateQueue.put(responseState.Past);
                        } else if (text.equals(KILLED)) {
                            gameStateQueue.put(gameState.Response);
                            responseStateQueue.put(responseState.Killed);
                        } else if (text.equals(LOSE)) {
                            gameStateQueue.put(gameState.End);
                            responseStateQueue.put(responseState.Lose);
                        } else if (text.equals(WIN)) {
                            gameStateQueue.put(gameState.End);
                            responseStateQueue.put(responseState.Win);
                        } else if (text.equals(OPPONENT_LEFT)) {
                            gameStateQueue.put(gameState.End);
                            responseStateQueue.put(responseState.Opponent_left);
                        } else if (text.startsWith(OPPONENT_TURN)) {
                            String[] response = text.split(" ");
                            turnX = Integer.parseInt(response[2]);
                            turnY = Integer.parseInt(response[3]);
                            logger.info(text);
                            gameStateQueue.put(gameState.Response);
                            responseStateQueue.put(responseState.Opponent_turn);
                        }
                    }
                    isOnline = false;
                    logger.info("Игра закончилась потоком прослушивания");

                } catch (IOException e) {
                    logger.error("Ошибка чтения клиента сервером", e);
                } catch (InterruptedException e) {
                    logger.error("Ошибка прерывания очереди", e);
                } finally {
                    if (inputStream != null) inputStream.close();
                    try {
                        server.close();
                    } catch (IOException ex) {
                        logger.error("Не удалось закрыть соединение с сервером", ex);
                    }
                }
            }).start();


            logger.info("Ожидание противника");
            Platform.runLater(() -> controller.setLabelText(WAITING_FOR_OPPONENT));
            Platform.runLater(() -> controller.setResponseLabelText(""));


            currentGameState = gameStateQueue.take();


            if (currentGameState != gameState.End) {
                logger.info("Противник найден");

                outputStream.println(getBattleshipsCoordinates());
                outputStream.flush();

                logger.info("Отправлены начальные координаты");
            }


            while (!isEnd || !gameStateQueue.isEmpty()) {

                logger.info("Дождитесь ответа");
                currentGameState = gameStateQueue.take();

                logger.info("game state " + currentGameState);

                if (currentGameState == gameState.Response) {

                    serverResponse = responseStateQueue.take();
                    logger.info("server response " + serverResponse);

                    if (serverResponse == responseState.Past) {

                        Platform.runLater(() -> controller.setButtonAppearance(MainController.cellState.shoot));
                        Platform.runLater(() -> controller.setLabelText(OPPONENT_TURN));
                        Platform.runLater(() -> controller.setResponseLabelText(PAST));

                    } else if (serverResponse == responseState.Wounded) {

                        Platform.runLater(() -> controller.setButtonAppearance(MainController.cellState.shootShip));
                        Platform.runLater(() -> controller.setResponseLabelText(WOUNDED));

                        gameStateQueue.put(gameState.Turn);

                    } else if (serverResponse == responseState.Killed) {

                        Platform.runLater(() -> controller.setButtonAppearance(MainController.cellState.shootShip));
                        Platform.runLater(() -> controller.setResponseLabelText(KILLED));
                        Platform.runLater(() -> controller.surroundKilledShip(controller.getOpponentGrid(),
                                controller.getLastTurnX(), controller.getLastTurnY()));

                        gameStateQueue.put(gameState.Turn);

                    } else if (serverResponse == responseState.Opponent_turn) {
                        Platform.runLater(() -> controller.changeButton(turnX, turnY));
                    }

                } else if (currentGameState == gameState.Turn) {

                    Platform.runLater(() -> controller.setLabelText(TURN));

                    Platform.runLater(() -> controller.setCanMakeTurn(true));

                    String turn = controller.getTurn();
                    if (!isOnline) {
                        turn = LEAVE;
                    }
                    outputStream.println(turn);

                    Platform.runLater(() -> controller.setCanMakeTurn(false));
                    outputStream.flush();
                    Platform.runLater(() -> controller.setLabelText("Ожидание ответа сервера..."));

                } else if (currentGameState == gameState.End) {
                    isEnd = true;
                }
            }

            serverResponse = responseStateQueue.take();

            if (serverResponse == responseState.Win) {
                Platform.runLater(() -> controller.setButtonAppearance(MainController.cellState.shootShip));
                Platform.runLater(() -> controller.setLabelText(WIN));
                Platform.runLater(() -> controller.surroundKilledShip(controller.getOpponentGrid(),
                        controller.getLastTurnX(), controller.getLastTurnY()));
            } else if (serverResponse == responseState.Lose) {
                Platform.runLater(() -> controller.setLabelText(LOSE));
            } else if (serverResponse == responseState.Opponent_left) {
                Platform.runLater(() -> controller.setLabelText(OPPONENT_LEFT));
            }

            logger.info("Game ended");
            outputStream.close();

        } catch (IOException e) {
            logger.error("Не удалось подключиться к серверу", e);
            Platform.runLater(() -> controller.setLabelText(FAILED_TO_CONNECT));
            Platform.runLater(() -> controller.setResponseLabelText(""));
        } catch (InterruptedException e) {
            logger.error("Ошибка прерывания очереди", e);
        }
    }

    private String getBattleshipsCoordinates() {

        logger.info("getBattleshipsCoordinates");

        int[][] field = new int[10][10];

        int[] ships = new int[]{5, 4, 3, 3, 2};

        StringBuilder stringBuilder = new StringBuilder();

        List<InitPoints> directions = new ArrayList<>(
                Arrays.asList(
                        new InitPoints(0, 1),
                        new InitPoints(0, -1),
                        new InitPoints(1, 0),
                        new InitPoints(-1, 0)
                )
        );

        int x, y, dx, dy, ix, iy;
        boolean isShipSet;
        boolean changeDirection;
        List<InitPoints> tempList = new ArrayList<>();
        InitPoints point;
        Random rand = new Random();
        int index;

        for (int ship : ships) {

            isShipSet = false;

            while (!isShipSet) {

                tempList.clear();
                tempList.addAll(directions);

                changeDirection = false;

                do {
                    ix = rand.nextInt(10);
                    iy = rand.nextInt(10);
                } while (field[ix][iy] != 0);


                while (tempList.size() > 0 && !isShipSet) {

                    x = ix;
                    y = iy;

                    index = rand.nextInt(tempList.size());
                    point = tempList.get(index);
                    tempList.remove(index);

                    dx = point.getX();
                    dy = point.getY();

                    int k;
                    for (k = 1; k < ship && !changeDirection; ++k) {

                        x += dx;
                        y += dy;

                        if (!inBounds(MIN_CELL, MAX_CELL, x) || !inBounds(MIN_CELL, MAX_CELL, y) || field[x][y] != 0) {
                            changeDirection = true;
                            break;
                        }
                    }

                    if (changeDirection) {
                        continue;
                    }

                    if (k == ship) {
                        stringBuilder.append(min(ix, x))
                                .append(" ").append(min(iy, y)).append(" ")
                                .append(max(ix, x)).append(" ").append(max(iy, y)).append("#");

                        int xFrom = min(x, ix) - 1;
                        int xTo = max(x, ix) + 1;
                        int yFrom = min(y, iy) - 1;
                        int yTo = max(y, iy) + 1;

                        for (int xi = xFrom; xi <= xTo; ++xi) {
                            if (!inBounds(MIN_CELL, MAX_CELL, xi)) {
                                continue;
                            }
                            for (int yi = yFrom; yi <= yTo; ++yi) {
                                if (!inBounds(MIN_CELL, MAX_CELL, yi)) {
                                    continue;
                                }
                                field[xi][yi] = 1;
                            }
                        }

                        isShipSet = true;
                    }
                }
            }
        }

        logger.info("Field sent by getBattleships:\n" + stringBuilder);
        Platform.runLater(() -> controller.setBattleships(stringBuilder.toString()));

        return stringBuilder.toString();
    }

    private void launch() throws IOException {
        new JFXPanel();
        Platform.runLater(() -> {

            try {
                Stage stage = new Stage();
                FXMLLoader fxmlLoader = new FXMLLoader(Client.class.getClassLoader().getResource("sea-battle.fxml"));

                Scene scene = new Scene(fxmlLoader.load(), 1280, 768);
                controller = fxmlLoader.getController();

                System.out.println(controller.toString());

                stage.setTitle("Sea Battle");
                stage.setScene(scene);
                stage.setResizable(false);
                stage.setOnHidden((e) -> {
                    controller.shutdown();
                    isOnline = false;
                    isEnd = true;
                    try {
                        gameStateQueue.put(gameState.End);
                        responseStateQueue.put(responseState.Lose);
                    } catch (InterruptedException ex) {
                        logger.error("Не удается добавить в очередь");
                    }
                    logger.info("shutdown");
                });
                stage.show();
            } catch (IOException e) {
                logger.error("Не удалось загрузить приложение", e);
            }
        });
    }

    private boolean inBounds(int from, int to, int num) {
        return from <= num && num < to;
    }
}