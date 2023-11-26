package ru.mai.lessons.rpks.client_handler;

import org.apache.log4j.Logger;
import ru.mai.lessons.rpks.server.Server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ClientHandler {
    private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());
    private Socket client;
    private Server server;
    private int clientId;
    private String name = "###";

    public ClientHandler(Socket client, Server server, int clientId) {
        this.client = client;
        this.server = server;
        this.clientId = clientId;
    }

    public void handle() {
        try (Scanner inputStream = new Scanner(client.getInputStream())) {
            while (inputStream.hasNext()) {
                String text = inputStream.nextLine();
                logger.info("Сообщение от клиента " + clientId + ": " + text);
            }
        } catch (IOException e) {
            logger.error("Ошибка при работе с клиентом", e);
        } finally {
            if (client != null) {
                try {
                    client.close();
                } catch (IOException ex) {
                    logger.error("Ошибка при закрытии клиента!", ex);
                }
            }
        }
    }

    public void sendMessage(String message) {
        PrintWriter outputStream = null;
        try {
            outputStream = new PrintWriter(client.getOutputStream());
            outputStream.println(message);
            outputStream.flush();
        } catch (IOException e) {
            logger.error("Проблема при записи сообщения в поток клиента: " + client.toString(), e);
        }
    }

    public void closeConnection() {
        if (client != null) {
            try {
                client.close();
            } catch (IOException ex) {
                logger.error("Ошибка при закрытии клиента!", ex);
            }
        }
    }

    public String getName() {
        return name;
    }
}