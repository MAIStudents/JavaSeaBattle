package ru.mai.lessons.rpks;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;


public class Server {
    public static final int port = 8080;
public static CopyOnWriteArrayList<ServerHandler> serverList = new CopyOnWriteArrayList<>();

    public static void main(String[] args) throws IOException {
        ServerSocket server = new ServerSocket(port, 2);
        System.out.println("Server started");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println();
            for (ServerHandler vr : serverList) {
                vr.send("depth");
                vr.downService();
            }

        }));

        try {
            while (true) {
                System.out.println("size: " + serverList.size());
                Socket socket = server.accept();
                if (serverList.size() < 2) {

                    try {
                        serverList.add(new ServerHandler(socket));
                    } catch (IOException e) {
                        socket.close();
                    }
                } else {
                    System.out.println("Connection rejected, max count of clients reached");
                    try {
                        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                        System.out.println("closed");
                        out.write("closed" + "\n");
                        out.flush();
                        out.close();
                    } catch (IOException e) {
                        System.err.println("Failed to send rejection message to client: " + e.getMessage());
                    } finally {
                        socket.close();
                    }
                }

            }
        } finally {
            server.close();
        }


    }


}


class ServerHandler extends Thread {
    private final Socket socket;

    private final BufferedReader in;
    private final BufferedWriter out;

    private static final long TIMEOUT = 20000;
    private static final long TIMEOUT_CONNECTION = 15000;
    private Timer timerForGame;
    private final Timer timerForConnection;

    private static final AtomicInteger countReady = new AtomicInteger(0);
    private static volatile boolean gameStarted = false;
    private boolean isReady = false;


    public ServerHandler(Socket newSocket) throws IOException {
        this.socket = newSocket;
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        start();
        timerForGame = new Timer();
        timerForConnection = new Timer();
        startTimerConnection();
    }

    private void startTimerConnection() {
        timerForConnection.schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println("Connection timeout.");
                for (ServerHandler vr : Server.serverList) {
                    vr.send("to");
                }

                downService();
            }
        }, TIMEOUT_CONNECTION);
    }

    private void startTimeoutTimer() {

        timerForGame.schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println("Client timeout.");
                for (ServerHandler vr : Server.serverList) {
                    vr.send("to");
                }

                downService();
            }
        }, TIMEOUT);
    }

    @Override
    public void run() {
        String word;
        try {
            while (true) {

                    word = in.readLine();
                    if (word == null || word.equals("exit") || word.equals("win")) {

                        System.out.println("end");

                        String result;
                        if (word != null && word.equals("win")) {
                            result = "win";
                        } else {
                            result = "exit";
                        }
                        for (ServerHandler vr : Server.serverList) {
                            if (!vr.equals(this)) {
                                vr.send(result);
                            }
                        }

                        this.downService();
                        break;
                    }
                    resetTimeoutTimer();
                    System.out.println("echo:" + word);

                    if (word.equals("ready")) {
                        if (!isReady) {
                            isReady = true;
                            int count = countReady.incrementAndGet();
                            System.out.println("Client is ready. Ready count: " + count);

                            if (count == 2 && !gameStarted) {
                                gameStarted = true;
                                startTimeoutTimer();

                                for (ServerHandler vr : Server.serverList) {
                                    vr.timerForConnection.cancel();
                                }
                                System.out.println("timer over");
                                Server.serverList.get(0).send("hurt:-1,-1");
                                Server.serverList.get(1).send("miss:-1,-1");
                            }
                        }
                    } else {
                        for (ServerHandler vr : Server.serverList) {
                            if (!vr.equals(this)) {
                                vr.send(word);
                            }
                        }
                    }

            }
        } catch (IOException e) {
            System.out.println("Closing connection.");
            this.downService();
        }
    }

    private void resetTimeoutTimer() {
        if (timerForGame != null) {
            timerForGame.cancel();
        }
        timerForGame = new Timer();
        startTimeoutTimer();
    }


    public void send(String msg) {
        try {
            out.write(msg + "\n");
            out.flush();
        } catch (IOException e) {
            System.out.println("stream close");
        }
    }

    public void downService() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
                if (timerForGame != null) {
                    timerForGame.cancel();
                }

                if (isReady) {
                    isReady = false;
                    countReady.decrementAndGet();
                }

                this.timerForConnection.cancel();
                Server.serverList.remove(this);

                if (Server.serverList.size() < 2) {
                    gameStarted = false;
                    System.out.println("Game stopped due to disabled client");
                    for (ServerHandler vr : Server.serverList) {
                        vr.send("interrupt");
                    }
                    Server.serverList.clear();
                    countReady.set(0);
                }
            }
        } catch (IOException e) {
            System.out.println("uer");
        }
    }
}


