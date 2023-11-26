package ru.mai.lessons.rpks.client;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;


public class Client {
    private static final Logger logger = Logger.getLogger(Client.class.getName());

    private String host = "localhost";
    private Integer port = 8843;

    public Client() {
    }

    public Client(String host, Integer port) {
        this.host = host;
        this.port = port;
    }

    public void start() {
        try {
            logger.info("Клиент инициализирован");
            Socket server = new Socket(host, port);
            PrintWriter outputStream = new PrintWriter(server.getOutputStream());
            outputStream.println("Привет! Я новый клиент!");
            outputStream.flush();

            try (Scanner inputStream = new Scanner(server.getInputStream())) {
                while (inputStream.hasNext()) {
                    String text = inputStream.nextLine();
                    System.out.println(text);
                }
            } catch (IOException e) {
                logger.error("Проблема при чтении сервера клиентом", e);
            }

            outputStream.close();
        } catch (IOException e) {
            logger.error("Ошибка при подключении к серверу", e);
        }
    }
}