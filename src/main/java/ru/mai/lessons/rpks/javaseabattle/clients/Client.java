package ru.mai.lessons.rpks.javaseabattle.clients;

import javafx.application.Platform;

import javafx.embed.swing.JFXPanel;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.log4j.Logger;
import ru.mai.lessons.rpks.javaseabattle.SeaBattleController;
import ru.mai.lessons.rpks.javaseabattle.commons.PointsImpl;

import java.awt.*;
import java.util.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;


import static java.lang.Math.*;
import static ru.mai.lessons.rpks.javaseabattle.commons.Variables.*;


public class Client {

    private static final Logger logger = Logger.getLogger(Client.class.getName());

    private String host = "localhost";
    private Integer port = 8832;
    private String name = "Bot";
    private SeaBattleController controller;

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
        Enemy_left,
        Enemy_turn
    }

    private gameState currentGameStatus;
    private responseState serverResponse;
    private boolean isOnline;
    private int turnX;
    private int turnY;
    private boolean isEnd;

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

            logger.info("Client initialized");
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

                        logger.info("Response " + text);

                        if (text.equals(TURN)) {
                            gameStateQueue.put(gameState.Turn);
                        } else if (text.equals(GOTHIM)) {

                            gameStateQueue.put(gameState.Response);
                            responseStateQueue.put(responseState.Wounded);
                        } else if (text.equals(MISS)) {
                            gameStateQueue.put(gameState.Response);
                            responseStateQueue.put(responseState.Past);
                        } else if (text.equals(DEADNOW)) {
                            gameStateQueue.put(gameState.Response);
                            responseStateQueue.put(responseState.Killed);
                        } else if (text.equals(LOSER)) {
                            gameStateQueue.put(gameState.End);
                            responseStateQueue.put(responseState.Lose);
                        } else if (text.equals(WINNER)) {
                            gameStateQueue.put(gameState.End);
                            responseStateQueue.put(responseState.Win);
                        } else if (text.equals(OPPONENT_LEFT)) {
                            gameStateQueue.put(gameState.End);
                            responseStateQueue.put(responseState.Enemy_left);
                        } else if (text.startsWith(OPPONENT_IS_THINKING)) {
                            String[] response = text.split(" ");
                            turnX = Integer.parseInt(response[2]);
                            turnY = Integer.parseInt(response[3]);
                            logger.info(text);
                            gameStateQueue.put(gameState.Response);
                            responseStateQueue.put(responseState.Enemy_turn);
                        }
                    }
                    isOnline = false;
                    logger.info("Game ended listening thread");

                } catch (IOException e) {
                    logger.error("Failed reading client by server", e);
                } catch (InterruptedException e) {
                    logger.error("Queue interrupted error", e);
                } finally {
                    if (inputStream != null) inputStream.close();
                    try {
                        server.close();
                    } catch (IOException ex) {
                        logger.error("Failed to close connection to server", ex);
                    }
                }
            }).start();


            logger.info("Waiting for opponent");
            Platform.runLater(() -> controller.setLabelText("Opponent's turn"));
            Platform.runLater(() -> controller.setResponseLabelText(""));


            currentGameStatus = gameStateQueue.take();


            if (currentGameStatus != gameState.End) {
                logger.info("Opponent found");

                outputStream.println(generateBattlefield());
                outputStream.flush();

                logger.info("Sent initial coordinates");
            }


            while (!isEnd || !gameStateQueue.isEmpty()) {

                logger.info("Wait for response");
                currentGameStatus = gameStateQueue.take();

                logger.info("game state " + currentGameStatus);

                if (currentGameStatus == gameState.Response) {

                    serverResponse = responseStateQueue.take();
                    logger.info("server response " + serverResponse);

                    if (serverResponse == responseState.Past) {

                        Platform.runLater(() -> controller.setButtonAppearance(SeaBattleController.cellState.shoot));
                        Platform.runLater(() -> controller.setLabelText("Opponent's turn"));
                        Platform.runLater(() -> controller.setResponseLabelText(MISS));

                    } else if (serverResponse == responseState.Wounded) {

                        Platform.runLater(() -> controller.setButtonAppearance(SeaBattleController.cellState.shootShip));
                        Platform.runLater(() -> controller.setResponseLabelText(GOTHIM));

                        gameStateQueue.put(gameState.Turn);

                    } else if (serverResponse == responseState.Killed) {

                        Platform.runLater(() -> controller.setButtonAppearance(SeaBattleController.cellState.shootShip));
                        Platform.runLater(() -> controller.setResponseLabelText(DEADNOW));
                        Platform.runLater(() -> controller.surroundKilledShip(controller.getOpponentGrid(),
                                controller.getLastTurnX(), controller.getLastTurnY()));

                        gameStateQueue.put(gameState.Turn);

                    } else if (serverResponse == responseState.Enemy_turn) {
                        Platform.runLater(() -> controller.changeButton(turnX, turnY));
                    }

                } else if (currentGameStatus == gameState.Turn) {

                    Platform.runLater(() -> controller.setLabelText(TURN));

                    Platform.runLater(() -> controller.setCanMakeTurn(true));

                    String turn = controller.getTurn();
                    if (!isOnline) {
                        turn = LEAVER;
                    }
                    outputStream.println(turn);

                    Platform.runLater(() -> controller.setCanMakeTurn(false));
                    outputStream.flush();
                    Platform.runLater(() -> controller.setLabelText("Waiting for server start..."));

                } else if (currentGameStatus == gameState.End) {
                    isEnd = true;
                }
            }

            serverResponse = responseStateQueue.take();

            if (serverResponse == responseState.Win) {
                Platform.runLater(() -> controller.setButtonAppearance(SeaBattleController.cellState.shootShip));
                Platform.runLater(() -> controller.setLabelText(WINNER));
                Platform.runLater(() -> controller.surroundKilledShip(controller.getOpponentGrid(),
                        controller.getLastTurnX(), controller.getLastTurnY()));
            } else if (serverResponse == responseState.Lose) {
                Platform.runLater(() -> controller.setLabelText(LOSER));
            } else if (serverResponse == responseState.Enemy_left) {
                Platform.runLater(() -> controller.setLabelText(OPPONENT_LEFT));
            }

            logger.info("Game ended");
            outputStream.close();

        } catch (IOException e) {
            logger.error("Failed to connect...", e);
            Platform.runLater(() -> controller.setLabelText("Failed to connect"));
            Platform.runLater(() -> controller.setResponseLabelText(""));
        } catch (InterruptedException e) {
            logger.error("Queue interrupted error", e);
        }
    }

    private String generateBattlefield() {

        logger.info("generating Battlefield");

        int[][] field = new int[10][10];

        int[] ships = new int[]{4, 3, 3, 2, 2, 2, 1, 1, 1, 1};
        Arrays.sort(ships);

        StringBuilder stringBuilder = new StringBuilder();

        List<PointsImpl> direction = new ArrayList<>(
                Arrays.asList(
                        new PointsImpl(0, 1),
                        new PointsImpl(0, -1),
                        new PointsImpl(1, 0),
                        new PointsImpl(-1, 0)
                )
        );

        int x, y, dx, dy, ix = 0, iy = 0;
        boolean shipsWasSet;
        boolean changeDirection;
        List<PointsImpl> uncheckedDirections = new ArrayList<>();
        PointsImpl point;
        Random rand = new Random();
        int index;


        for (int ship : ships) {

            shipsWasSet = false;


            while (!shipsWasSet) {


                uncheckedDirections.clear();
                uncheckedDirections.addAll(direction);

                ix = rand.nextInt(10);
                iy = rand.nextInt(10);
                while (field[ix][iy] > 0) {
                    ix = rand.nextInt(10);
                    iy = rand.nextInt(10);
                }


                while (uncheckedDirections.size() > 0 && !shipsWasSet) {

                    x = ix;
                    y = iy;

                    changeDirection = false;
                    index = rand.nextInt(uncheckedDirections.size());
                    point = uncheckedDirections.get(index);
                    uncheckedDirections.remove(index);

                    dx = point.getX();
                    dy = point.getY();

                    int k;
                    for (k = 1; k < ship && !changeDirection; ++k) {

                        x += dx;
                        y += dy;

                        if (!(0 <= x && x < 10) || !(0 <= y && y < 10) || field[x][y] != 0) {
                            changeDirection = true;
                            break;
                        }
                    }

                    if (changeDirection) {
                        continue;
                    }

                    if (k == 1 && field[x][y] > 0) {
                        break;
                    }


                    stringBuilder.append(min(ix, x))
                            .append(" ").append(min(iy, y)).append(" ")
                            .append(max(ix, x)).append(" ").append(max(iy, y)).append("#");

                    int xFrom = min(x, ix) - 1;
                    int xTo = max(x, ix) + 1;
                    int yFrom = min(y, iy) - 1;
                    int yTo = max(y, iy) + 1;

                    for (int xi = xFrom; xi <= xTo; ++xi) {
                        if (!(0 <= xi && xi < 10)) {
                            continue;
                        }
                        for (int yi = yFrom; yi <= yTo; ++yi) {
                            if (!(0 <= yi && yi < 10)) {
                                continue;
                            }
                            field[xi][yi] = 1;
                        }
                    }

                    for (int xi = xFrom + 1; xi <= xTo - 1; ++xi) {
                        if (!(0 <= xi && xi < 10)) {
                            continue;
                        }
                        for (int yi = yFrom + 1; yi <= yTo - 1; ++yi) {
                            if (!(0 <= yi && yi < 10)) {
                                continue;
                            }
                            field[xi][yi] = 2;
                        }
                    }

                    shipsWasSet = true;
                }
            }
        }

        logger.info("Generated battlefield by generateBattlefield:\n" + stringBuilder);
        Platform.runLater(() -> controller.setBattleships(stringBuilder.toString()));

        return stringBuilder.toString();
    }

    private void launch() throws IOException {
        new JFXPanel();
        Platform.runLater(() -> {

            try {
                Stage stage = new Stage();
                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("sea-battle.fxml"));

                Scene scene = new Scene(fxmlLoader.load(), 1280, 768);
                controller = fxmlLoader.getController();

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
                        logger.error("Unable to add to queue");
                    }
                    logger.info("shutdown");
                });
                stage.show();
            } catch (IOException e) {
                logger.error("Failed to load application", e);
            }
        });
    }
}
