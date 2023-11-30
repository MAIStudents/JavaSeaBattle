package ru.mai.lessons.rpks.server;

import javafx.application.Platform;
import javafx.concurrent.Task;
import org.apache.log4j.Logger;
import ru.mai.lessons.rpks.client_handler.ClientHandler;
import ru.mai.lessons.rpks.message.Message;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Server {
    private static final Logger logger = Logger.getLogger(Server.class.getName());
    private String host = "localhost";
    private Integer port = 8843;
    private final List<ClientHandler> clients = new ArrayList<>();
    private int clientId = 1;
    private boolean isAlive = true;
    private final ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private final boolean[] busyIds = new boolean[]{false, false};
    protected boolean step;

    public Server() {

    }

    public Server(String host, Integer port) {
        this.host = host;
        this.port = port;
    }

    public void start() {
        new Thread(() -> {
            logger.info("Инициализация сервера");

            try (ServerSocket serverSocket = new ServerSocket(port)) {
                serverSocket.setSoTimeout(100);
                logger.info("Сервер стартовал и ожидает подключение клиента");

                while (isAlive) {
                    try {
                        Socket socketForClient = serverSocket.accept();
                        logger.info("Подключился новый клиент: " + socketForClient.toString());
                        ClientHandler clientHandler = new ClientHandler(socketForClient, socketForClient.getOutputStream(), socketForClient.getInputStream(), this, getId());
                        clients.add(clientHandler);
                        service.execute(clientHandler::handle);
                    } catch (SocketTimeoutException e) {
                        //Нужно для того, чтобы можно было остановить сервер
                    }
                }

                logger.info("Сервер остановлен");
                shutdown();
            } catch (IOException ex) {
                logger.error("Проблема с сервером", ex);
            }
        }).start();
    }

    public boolean isAlive() {
        return isAlive;
    }

    public void stop() {
        isAlive = false;
    }

    public void shutdown() {
        for (ClientHandler client : clients) {
            client.closeConnection();
        }

        service.shutdown();

        try {
            if (!service.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
                service.shutdownNow();
            }
        } catch (InterruptedException e) {
            service.shutdownNow();
        }
    }

    public int getId() {
        for (int i = 0; i < busyIds.length; i++) {
            if (!busyIds[i]) {
                busyIds[i] = true;
                return i + 1;
            }
        }

        return 0;
    }

    public void clientDisconnect(int clientId) {
        busyIds[clientId - 1] = false;

        for (ClientHandler client : clients) {
            if (client.getClientId() == clientId) {
                clients.remove(client);
                return;
            }
        }
    }

    public List<ClientHandler> getClients() {
        return clients;
    }

    public void doStep() {
        logger.error("Клиент совершил ход");
        step = !step;
    }
}
