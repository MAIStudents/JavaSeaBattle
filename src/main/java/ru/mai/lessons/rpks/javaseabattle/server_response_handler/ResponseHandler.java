package ru.mai.lessons.rpks.javaseabattle.server_response_handler;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import ru.mai.lessons.rpks.javaseabattle.Main;
import ru.mai.lessons.rpks.javaseabattle.controller.Controller;
import ru.mai.lessons.rpks.javaseabattle.controller.game_control_button_state.GameControlButtonState;

import java.io.*;
import java.net.Socket;

public class ResponseHandler implements Runnable {

    private final Socket socket;

    private final Controller controller;

    public ResponseHandler(Socket socket, Controller controller) {
        this.socket = socket;
        this.controller = controller;
    }

    public String getMessageFromServer() throws IOException {
        var inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        return inputStream.readLine();
    }

    public void sendMessageToServer(String message) throws IOException {
        var outputStream = new PrintWriter(socket.getOutputStream());
        outputStream.println(message);
        outputStream.flush();
    }

    @Override
    public void run() {
        try {
            while (!socket.isClosed()) {
                String responseFromServer = getMessageFromServer();
                if (responseFromServer == null) {
                    break;
                }
                if (responseFromServer.equals("WON")) {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle(Main.BUNDLE.getString("alert.looseMessageTitle"));
                        alert.setHeaderText(Main.BUNDLE.getString("alert.looseMessageText"));
                        alert.show();
                        controller.gameControlButton.setText(Main.BUNDLE.getString("loose.gameControlButtonText"));
                        controller.hint.setText(Main.BUNDLE.getString("loose.hintText"));
                        controller.gameControlButtonState = GameControlButtonState.QUIT;
                    });
                } else if (responseFromServer.startsWith("SHOT")) {
                    Platform.runLater(() -> {
                        try {
                            var arguments = responseFromServer.split(" ");
                            int row = Integer.parseInt(arguments[1]);
                            int column = Integer.parseInt(arguments[2]);
                            int sinkFlag = Integer.parseInt(arguments[3]);
                            boolean isGameWon = false;
                            if (sinkFlag == 1) {
                                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                alert.setTitle(Main.BUNDLE.getString("alert.shipSinkTitleGood"));
                                alert.setHeaderText(Main.BUNDLE.getString("alert.shipSinkTextGood"));
                                alert.show();
                                int shipType = Integer.parseInt(arguments[4]);
                                isGameWon = controller.markShipAsDefeated(shipType);
                            }
                            controller.getOpponentTableNode(row, column).setStyle(Controller.SHOT_SHIP_CELL_TYPE);
                            if (!isGameWon) {
                                controller.canAttackOpponent = true;
                                controller.hint.setText(Main.BUNDLE.getString("yourTurn.hintText"));
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                } else if (responseFromServer.startsWith("MISSED")) {
                    Platform.runLater(() -> {
                        var arguments = responseFromServer.split(" ");
                        int row = Integer.parseInt(arguments[1]);
                        int column = Integer.parseInt(arguments[2]);
                        controller.getOpponentTableNode(row, column).setStyle(Controller.MISSED_CELL_TYPE);
                        controller.canAttackOpponent = false;
                        controller.hint.setText(Main.BUNDLE.getString("opponentTurn.hintText"));
                    });
                } else if (responseFromServer.startsWith("ORDER")) {
                    Platform.runLater(() -> {
                        controller.canAttackOpponent = Integer.parseInt(responseFromServer.split(" ")[1]) == 1;
                        if (controller.canAttackOpponent) {
                            controller.hint.setText(Main.BUNDLE.getString("yourTurn.hintText"));
                        } else {
                            controller.hint.setText(Main.BUNDLE.getString("opponentTurn.hintText"));
                        }
                        controller.gameControlButton.setDisable(false);

                        controller.gameControlButtonState = GameControlButtonState.QUIT_WITH_LOOSING;
                        controller.gameControlButton.setText(Main.BUNDLE.getString("disconnectWithLoosing.gameControlButtonText"));
                    });
                } else if (responseFromServer.startsWith("SHOOT")) {
                    Platform.runLater(() -> {
                        try {
                            var arguments = responseFromServer.split(" ");
                            int row = Integer.parseInt(arguments[1]);
                            int column = Integer.parseInt(arguments[2]);
                            if (controller.getPlayerTableNode(row, column).getStyle().equals(Controller.SHIP_CELL_TYPE)) {
                                int sinkFlag = 0;
                                var shotShipData = controller.isShipSink(row, column);
                                if (shotShipData.getKey()) {
                                    sinkFlag = 1;
                                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                    alert.setTitle(Main.BUNDLE.getString("alert.shipSinkTitleBad"));
                                    alert.setHeaderText(Main.BUNDLE.getString("alert.shipSinkTextBad"));
                                    alert.show();
                                }
                                controller.getPlayerTableNode(row, column).setStyle(Controller.SHOT_SHIP_CELL_TYPE);
                                sendMessageToServer("SHOT " + row + " " + column + " " + sinkFlag + " " + shotShipData.getValue());
                                controller.canAttackOpponent = false;
                                controller.hint.setText(Main.BUNDLE.getString("opponentTurn.hintText"));
                            } else {
//                                controller.getPlayerTableNode(row, column).setStyle(Controller.MISSED_CELL_TYPE);
                                sendMessageToServer("MISSED " + row + " " + column);
                                controller.canAttackOpponent = true;
                                controller.hint.setText(Main.BUNDLE.getString("yourTurn.hintText"));
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                } else if (responseFromServer.equals("OPPONENT_DISCONNECTED")) {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle(Main.BUNDLE.getString("opponentDisconnected.winTitle"));
                        alert.setHeaderText(Main.BUNDLE.getString("opponentDisconnected.winText"));
                        alert.show();
                        controller.gameControlButton.setText(Main.BUNDLE.getString("opponentDisconnected.gameControlButtonText"));
                        controller.hint.setText(Main.BUNDLE.getString("opponentDisconnected.hintText"));
                        controller.gameControlButtonState = GameControlButtonState.QUIT;
                    });
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
