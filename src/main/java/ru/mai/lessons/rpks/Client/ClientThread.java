package ru.mai.lessons.rpks.Client;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Modality;
import ru.mai.lessons.rpks.Game.Game;

import java.io.*;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

import static ru.mai.lessons.rpks.Game.Game.*;

public class ClientThread extends Thread {
    private BufferedReader in;
    private BufferedWriter out;
    private static final Logger logger = Logger.getLogger(ClientThread.class.getName());
    private volatile boolean running = true;
    private Socket socket = null;
    public boolean isOpponentReady = false;

    public boolean isTurn = false;

    public ClientThread(Socket socket) throws IOException {
        if (!socket.isClosed()) {
            this.socket = socket;
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        }
    }

    @Override
    public void run() {
        try {
            while (running) {
                if (isOpponentReady) {
                    checkGameOver();
                }
                String message = in.readLine();
                if (message != null) {
                    handleMessage(message);
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error while trying to run", e);
        } finally {
            try {
                in.close();
                out.close();
                socket.close();
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error closing resources", e);
            }
        }
        stopThread();
    }

    public void stopThread() {
        running = false;
        this.interrupt();
    }

    private void checkGameOver() {
        if (!Game.playerHasShips() && Game.opponentHasShips()) {
            showAlert("Проигрыш", "Все твои корабли уничтожены. Удачи в следующий раз!", true);
        } else if (Game.playerHasShips() && !Game.opponentHasShips()) {
            Game.wins++;
            showAlert("Победа", "Поздравляем! Ты разрушил все корабли противника", true);
        }
    }

    private void showAlert(String title, String message, boolean sendMessage) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);

            alert.initModality(Modality.APPLICATION_MODAL);

            alert.showAndWait().ifPresent(e -> {
                isOpponentReady = false;
                isTurn = false;
                if (sendMessage) {
                    try {
                        sendMessage(NEW_GAME);
                    } catch (IOException ex) {
                        logger.log(Level.SEVERE, "Unable to send message", ex);
                    }
                }
                Platform.runLater(Game::showStartingScreen);
            });
        });
    }

    private void handleMessage(String message) throws IOException {
        logger.log(Level.INFO, "Message received: " + message);
        String GAME_OVER = "Игра окончена";
        if (message.startsWith(SHOT_RESULT)) {
            Game.setShotOnOpponentField(message);
            String[] result = message.split(" ");
            if (MISS.equals(result[3])) {
                isTurn = false;
                Game.updateTurnText();
            }
        } else if (message.startsWith(SHOT)) {
            Game.setShotOnField(message);
        } else if (message.equals(OPPONENT_READY)) {
            isOpponentReady = true;
            Platform.runLater(() -> {
                if (isOpponentReady) {
                    Game.startBattle();
                }
            });
        } else if (message.equals(TURN)) {
            isTurn = true;
            Game.updateTurnText();
        } else if (message.equals(CLOSE) && Game.isInBattle) {
            isTurn = false;
            showAlert(GAME_OVER, "Противник отключился", true);
        } else if (message.equals(STOP)) {
            Game.removeClientThread();
            if (Game.isInBattle || Game.isInShipPlacement) {
                showAlert(GAME_OVER, "Потеряно соединение с сервером", false);
            }
        }
    }

    public void sendMessage(String message) throws IOException {
        String[] result = message.split(" ");
        if (message.startsWith(SHOT_RESULT) && MISS.equals(result[3])) {
            isTurn = true;
            Game.updateTurnText();
        }
        out.write(message + "\n");
        out.flush();
        logger.log(Level.INFO, "Message sent: " + message);
    }
}
