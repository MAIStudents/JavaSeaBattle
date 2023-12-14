package ru.mai.lessons.rpks;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {

    private static final Logger logger = Logger.getLogger(Server.class.getName());

    private String host = "localhost"; //127.0.0.1
    private Integer port = 8843;

    private List<ClientHandler> clients = new ArrayList<>();

    public Server() {
    }

    public Server(String host, Integer port) {
        this.host = host;
        this.port = port;
    }

    public void start() {
        logger.info("Инициализация сервера");
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(port);
            logger.info("Сервер стартовал и ожидает подключение клиента");
            while (true) {
                Socket client = serverSocket.accept();
                logger.info("Подключился новый клиент: " + client.toString());
//                Scanner inputStream = new Scanner(client.getInputStream());
//                while (inputStream.hasNext()) {
//                    String text = inputStream.nextLine();
//                    logger.info("Сообщение от клиента: " + text);
//                    PrintWriter outputStream = new PrintWriter(client.getOutputStream());
//                    outputStream.println("Эхо: " + text);
//                    outputStream.flush();
//                }
//                inputStream.close();
                ClientHandler clientHandler = new ClientHandler(client, this);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            logger.error("Проблема с сервером", e);
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException ex) {
                    logger.error("Проблема при закрытии сервера", ex);
                }
            }
        }
    }

    public void sendMessageToChat(String message, String name) {
        for (ClientHandler client : clients) {
            client.sendMessage(name + ": " + message);
        }
    }
}