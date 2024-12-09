package ru.mai.lessons.rpks.Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import static ru.mai.lessons.rpks.Game.Game.STOP;

public class Server {
    public static final int PORT = 5000;
    public static LinkedList<ServerThread> serverList = new LinkedList<>();
    private static final Logger logger = Logger.getLogger(Server.class.getName());
    private boolean isStopped = false;

    public static void main(String[] args) {
        Server server = new Server();
        Thread stopingThread = new Thread(() -> {
            try {
                System.out.println("Press Enter to stop the server...");
                int ignored = System.in.read();
                server.stop();
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Something went wrong", e);
            }
        });
        stopingThread.start();
        server.start();
        stopingThread.interrupt();
    }

    private void stop() {
        isStopped = true;
        try {
            closeThreads();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Can't close threads", e);
        }
        logger.log(Level.INFO, "Server is closed");
        System.exit(0);
    }

    public void start() {
        try (ServerSocket server = new ServerSocket(PORT)) {
            server.setSoTimeout(60000);
            getConnection(server);
            serverList.get(0).setOpponent(serverList.get(1));
            serverList.get(1).setOpponent(serverList.get(0));

            while (!isStopped) {
                int currentIndex = 0;
                for (ServerThread player : serverList) {
                    if (!player.isConnected()) {
                        logger.log(Level.INFO, "Player disconnected, attempting to reconnect...");
                        player.stopThread();
                        player.interrupt();
                        serverList.remove(currentIndex);
                        addPlayer(server);
                        serverList.get(0).setOpponent(serverList.get(1));
                        serverList.get(1).setOpponent(serverList.get(0));
                        break;
                    }
                    ++currentIndex;
                }
                Thread.sleep(3000);
            }
        } catch (SocketTimeoutException e) {
            logger.log(Level.INFO, "Server didn't get connections in 1 minute. Closing...");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Can't open server", e);
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "Thread was interrupted", e);
        }
        stop();
    }

    private void getConnection(ServerSocket server) throws IOException {
        while (serverList.size() != 2) {
            addPlayer(server);
        }
    }

    private void addPlayer(ServerSocket server) throws IOException {
        logger.log(Level.INFO, "Trying to connect player");
        Socket clientSocket = server.accept();
        ServerThread player = new ServerThread(clientSocket);
        player.start();
        logger.log(Level.INFO, "Player connected");
        serverList.add(player);
    }

    private void closeThreads() throws IOException {
        logger.log(Level.INFO, "Closing threads");
        for (ServerThread player : serverList) {
            if (player.isConnected()) {
                player.sendMessage(STOP);
            }
            player.interrupt();
        }
        logger.log(Level.INFO, "All threads are closed");
    }
}
