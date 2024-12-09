package ru.mai.lessons.rpks.Server;

import java.io.*;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

import static ru.mai.lessons.rpks.Game.Game.*;

public class ServerThread extends Thread {
    private final BufferedReader in;
    private final BufferedWriter out;
    private static final Logger logger = Logger.getLogger(ServerThread.class.getName());
    public boolean isStopped = false;
    private ServerThread opponent;
    public boolean isPlayerReady = false;
    private final Socket socket;

    private boolean isConnected = true;

    public boolean isTurn = false;

    private boolean isReadyMessageSent = false;

    public ServerThread(Socket socket) throws IOException {
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            while (!isStopped) {
                if (isConnected) {
                    checkGameStart();
                    checkIsTurn();
                    String message = in.readLine();
                    if (message != null) {
                        handleMessage(message);
                    }
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error while trying to run", e);
        } finally {
            try {
                stopThread();
            } catch (IOException e) {
                logger.log(Level.INFO, "Unable to stop thread");
            }
            logger.log(Level.INFO, "Player disconnected");
        }
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void stopThread() throws IOException {
        isStopped = true;
        cleanThread();
    }

    public void cleanThread() {
        cleanGame();
        isConnected = false;
        try {
            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error closing resources", e);
        }
    }

    public void cleanGame() {
        isReadyMessageSent = false;
        isTurn = false;
        isPlayerReady = false;
    }

    private void checkGameStart() throws IOException {
        if (!isReadyMessageSent && isPlayerReady && opponent.isPlayerReady) {
            sendMessageToOpponent(OPPONENT_READY);
            sendMessage(OPPONENT_READY);
            isReadyMessageSent = true;
            opponent.isReadyMessageSent = true;
        }
    }

    public void setOpponent(ServerThread opponent) {
        this.opponent = opponent;
    }

    private void handleMessage(String message) throws IOException {
        logger.log(Level.INFO, "Message received: " + message);
        if (message.startsWith(SHOT)) {
            sendMessageToOpponent(message);
        } else if (message.equals(READY)) {
            isPlayerReady = true;
            if (!opponent.isTurn) {
                isTurn = true;
            }
        } else if (message.startsWith(SHOT_RESULT)) {
            String[] result = message.split(" ");
            if (MISS.equals(result[3])) {
                isTurn = true;
                opponent.isTurn = false;
            }
            sendMessage(message);
        } else if (message.equals(CLOSE)) {
            sendMessageToOpponent(CLOSE);
            cleanGame();
        } else if (message.equals(STOP)) {
            if (opponent != null && opponent.isConnected && !opponent.socket.isClosed()) {
                sendMessageToOpponent(CLOSE);
            }
            cleanThread();
        } else if (message.equals(NEW_GAME)) {
            cleanGame();
        }
    }

    private void checkIsTurn() throws IOException {
        if (isTurn) {
            sendMessage(TURN);
        }
    }

    private void sendMessageToOpponent(String message) throws IOException {
        opponent.out.write(message + "\n");
        opponent.out.flush();
        logger.log(Level.INFO, "Message sent to opponent: " + message);
    }

    void sendMessage(String message) throws IOException {
        out.write(message + "\n");
        out.flush();
        logger.log(Level.INFO, "Message sent: " + message);
    }
}
