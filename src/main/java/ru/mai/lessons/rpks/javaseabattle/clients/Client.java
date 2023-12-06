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


import static java.lang.Math.max;
import static java.lang.Math.min;
import static ru.mai.lessons.rpks.javaseabattle.commons.Responses.*;


public class Client {

    private static final Logger logger = Logger.getLogger(Client.class.getName());

    private String host = "localhost";
    private Integer port = 8832;
    private String name = "Bot";
    private SeaBattleController controller;

    enum gameState {
        Turn,
        Wait,
        Response,
        End
    }

    enum responseState {
        Wait,
        Win,
        Lose,
        Past,
        Wounded,
        Killed
    }
    private gameState currentGameState;
    private responseState serverResponse;

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

        try {

            logger.info("Client initialized");
            Socket server = new Socket(host, port);

            PrintWriter outputStream = new PrintWriter(server.getOutputStream());
            outputStream.println("New player ###" + name);
            outputStream.flush();

            currentGameState = gameState.Wait;

            new Thread(() -> {
                Scanner inputStream = null;
                try {
                    inputStream = new Scanner(server.getInputStream());

                    while (inputStream.hasNext()) {

                        String text = inputStream.nextLine();

                        // get server response
                        logger.info("Response " + text);

                        if (text.equals(TURN)) {
                            currentGameState = gameState.Turn;
                        } else if (text.equals(WOUNDED)) {
                            currentGameState = gameState.Response;
                            serverResponse = responseState.Wounded;
                        } else if (text.equals(PAST)) {
                            currentGameState = gameState.Response;
                            serverResponse = responseState.Past;
                        } else if (text.equals(KILLED)) {
                            currentGameState = gameState.Response;
                            serverResponse = responseState.Killed;
                        } else if (text.equals(LOSE)) {
                            currentGameState = gameState.End;
                            serverResponse = responseState.Lose;
                        } else if (text.equals(WIN)) {
                            currentGameState = gameState.End;
                            serverResponse = responseState.Win;
                        } else {
                            logger.error("Can not recognize server response");
                        }
                    }
                } catch (IOException e) {
                    logger.error("Failed reading client by server", e);
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
            while (currentGameState == gameState.Wait) {
                // waiting for opponent
                try {
                    Thread.sleep(10L);
                } catch (InterruptedException e) {
                    logger.error("Thread sleep client " + name, e);
                }
            }

            logger.info("Opponent found");

            outputStream.println(getBattleshipsCoordinates());
            outputStream.flush();

            logger.info("Sent initial coordinates");

            currentGameState = gameState.Wait;


            while (currentGameState != gameState.End) {

                if (currentGameState == gameState.Wait) {

                    try {
                        Thread.sleep(10L);
                    } catch (InterruptedException e) {
                        logger.error("Thread sleep client " + name, e);
                    }

                } else if (currentGameState == gameState.Response) {

                    if (serverResponse == responseState.Past) {
                        // print new .
                        Platform.runLater(() -> controller.setButtonText("."));
                        Platform.runLater(() -> controller.setLabelText("Opponent's turn"));
                        currentGameState = gameState.Wait;
                        serverResponse = responseState.Wait;

                    } else if (serverResponse == responseState.Wounded) {
                        // print x
                        Platform.runLater(() -> controller.setButtonText("x"));
                        currentGameState = gameState.Turn;
                        serverResponse = responseState.Wait;

                    } else if (serverResponse == responseState.Killed) {
                        // print x and a lot of .
                        Platform.runLater(() -> controller.setButtonText("x"));
                        currentGameState = gameState.Turn;
                        serverResponse = responseState.Wait;

                    } else {
                        Platform.runLater(() -> controller.setLabelText("Waiting for server response..."));
                        try {
                            Thread.sleep(10L);
                        } catch (InterruptedException e) {
                            logger.error("Thread sleep client " + name, e);
                        }
                    }

                } else if (currentGameState == gameState.Turn) {

                    Platform.runLater(() -> controller.setLabelText("Your turn"));

                    Platform.runLater(() -> controller.setCanMakeTurn(true));
                    outputStream.println(controller.getTurn());
                    Platform.runLater(() -> controller.setCanMakeTurn(false));
                    outputStream.flush();
                    currentGameState = gameState.Response;
                }
            }

            if (serverResponse == responseState.Win) {
                Platform.runLater(() -> controller.setLabelText("You win!"));
            } else if (serverResponse == responseState.Lose) {
                Platform.runLater(() -> controller.setLabelText("You lose!"));
            }

            logger.info("Game ended");
            outputStream.close();

        } catch (IOException e) {
            logger.error("Failed to connect to server", e);
            Platform.runLater(() -> controller.setLabelText("Failed to connect"));
        }

    }

    private String getBattleshipsCoordinates() {

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

                Scene scene = new Scene(fxmlLoader.load(), 1280, 720);
                controller = fxmlLoader.getController();

                stage.setScene(scene);
                stage.setResizable(false);
                stage.show();
            } catch (IOException e) {
                logger.error("Failed loading application", e);
            }
        });
    }
}
