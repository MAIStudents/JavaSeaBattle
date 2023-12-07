package ru.mai.lessons.rpks.javaseabattle.clients;

import javafx.application.Platform;

import javafx.embed.swing.JFXPanel;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.log4j.Logger;
import ru.mai.lessons.rpks.javaseabattle.SeaBattleController;
import ru.mai.lessons.rpks.javaseabattle.commons.IntPoint;

import java.util.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;


import static java.lang.Math.*;
import static ru.mai.lessons.rpks.javaseabattle.commons.Responses.*;


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
        Opponent_left,
        Opponent_turn
    }
    private gameState currentGameState;
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
        } catch (IOException ignored) {}

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
                    logger.info("here 1");
                    while (inputStream.hasNext() && !isEnd) {
                        logger.info("here 2");
                        String text = inputStream.nextLine();
                        logger.info("here 3");

                        logger.info("Response " + text);

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
                    logger.info("Game ended listening thread");

                } catch (IOException e) {
                    logger.error("Failed reading client by server", e);
                } catch (InterruptedException e) {
                    logger.error("Synchronous queue interrupted", e);
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
            Platform.runLater(() -> controller.setLabelText("Waiting for opponent"));
            Platform.runLater(() -> controller.setResponseLabelText(""));


            currentGameState = gameStateQueue.take();


            if (currentGameState != gameState.End) {
                logger.info("Opponent found");

                outputStream.println(getBattleshipsCoordinates());
                outputStream.flush();

                logger.info("Sent initial coordinates");
            }


            while (!isEnd || !gameStateQueue.isEmpty()) {

                logger.info("Wait for response");
                currentGameState = gameStateQueue.take();

                logger.info("game state " + currentGameState);

                if (currentGameState == gameState.Response) {

                    serverResponse = responseStateQueue.take();
                    logger.info("server response " + serverResponse);

                    if (serverResponse == responseState.Past) {
                        // print new .
                        Platform.runLater(() -> controller.setButtonText("."));
                        Platform.runLater(() -> controller.setLabelText("Opponent's turn"));
                        Platform.runLater(() -> controller.setResponseLabelText("Past"));

                    } else if (serverResponse == responseState.Wounded) {
                        // print x
                        Platform.runLater(() -> controller.setButtonText("x"));
                        Platform.runLater(() -> controller.setResponseLabelText("Wounded"));

                        gameStateQueue.put(gameState.Turn);

                    } else if (serverResponse == responseState.Killed) {
                        // print x and a lot of .
                        Platform.runLater(() -> controller.setButtonText("x"));
                        Platform.runLater(() -> controller.setResponseLabelText("Killed"));
                        Platform.runLater(() -> controller.surroundKilledShip());

                        gameStateQueue.put(gameState.Turn);

                    } else if (serverResponse == responseState.Opponent_turn) {
                        Platform.runLater(() -> controller.changeButton(turnX, turnY));
                    }

                } else if (currentGameState == gameState.Turn) {

                    Platform.runLater(() -> controller.setLabelText("Your turn"));

                    Platform.runLater(() -> controller.setCanMakeTurn(true));

                    String turn = controller.getTurn();
                    if (!isOnline) {
                        turn = LEAVE;
                    }
                    outputStream.println(turn);

                    Platform.runLater(() -> controller.setCanMakeTurn(false));
                    outputStream.flush();
                    Platform.runLater(() -> controller.setLabelText("Waiting for server response..."));

                } else if (currentGameState == gameState.End) {
                    isEnd = true;
                }
            }

            serverResponse = responseStateQueue.take();

            if (serverResponse == responseState.Win) {
                Platform.runLater(() -> controller.setButtonText("x"));
                Platform.runLater(() -> controller.setLabelText("You win!"));
                Platform.runLater(() -> controller.surroundKilledShip());
            } else if (serverResponse == responseState.Lose) {
                Platform.runLater(() -> controller.setLabelText("You lose!"));
            } else if (serverResponse == responseState.Opponent_left) {
                Platform.runLater(() -> controller.setLabelText("Opponent left"));
            }

            logger.info("Game ended");
            outputStream.close();

        } catch (IOException e) {
            logger.error("Failed to connect to server", e);
            Platform.runLater(() -> controller.setLabelText("Failed to connect"));
            Platform.runLater(() -> controller.setResponseLabelText(""));
        } catch (InterruptedException e) {
            logger.error("Synchronous queue interrupted", e);
        }

    }

    private String getBattleshipsCoordinates() {
        logger.info("getBattleshipsCoordinates");

        int[][] field = new int[10][10];

        int[] ships = new int[] { 5, 4, 3, 3, 2 };

        StringBuilder stringBuilder = new StringBuilder();

        List<IntPoint> directions = new ArrayList<>(
                Arrays.asList(
                    new IntPoint(0, 1),
                    new IntPoint(0, -1),
                    new IntPoint(1, 0),
                    new IntPoint(-1, 0)
                )
        );

        int x, y, dx, dy, ix, iy;
        boolean areShipsSet;
        boolean changeDirection;
        List<IntPoint> tempList = new ArrayList<>();
        IntPoint point;
        Random rand = new Random();
        int index;

        for (int ship : ships) {

            areShipsSet = false;

            while (!areShipsSet) {

                tempList.clear();
                tempList.addAll(directions);

                changeDirection = false;

                ix = rand.nextInt(10);
                iy = rand.nextInt(10);
                x = ix;
                y = iy;

                while (tempList.size() > 0 && !areShipsSet) {

                    index = rand.nextInt(tempList.size());
                    point = tempList.get(index);
                    tempList.remove(index);

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

                        for (int i = x - 1; i <= x + 1 && !changeDirection; ++i) {

                            if (!(0 <= i && i < 10)) continue;
                            for (int j = y - 1; j <= y + 1; ++j) {

                                if (!(0 <= j && j < 10)) continue;
                                if (field[i][j] == 1) {
                                    changeDirection = true;
                                    break;
                                }
                            }
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

                        areShipsSet = true;
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
