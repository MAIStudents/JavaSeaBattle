package ru.mai.lessons.rpks;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;


public class Client {

    private static final Logger logger = Logger.getLogger(Client.class.getName());

    private String host = "localhost";
    private Integer port = 8843;
    private String name = "Bot";

    public Client() {
    }

    public Client(String host, Integer port, String name) {
        this.host = host;
        this.port = port;
        this.name = name;
    }

    public void start() {
        try {
            logger.info("Клиент инициализирован");
            Socket server = new Socket(host, port);
            PrintWriter outputStream = new PrintWriter(server.getOutputStream());
            outputStream.println("Привет! Я новый клиент! ###" + name);
            outputStream.flush();

            new Thread(() -> {
                Scanner inputStream = null;
                try {
                    inputStream = new Scanner(server.getInputStream());

                    while (inputStream.hasNext()) {
                        String text = inputStream.nextLine();
                        System.out.println(text);
                    }
                } catch (IOException e) {
                    logger.error("Проблема при чтении сервера клиентом", e);
                } finally {
                    if (inputStream != null) inputStream.close();
                    try {
                        server.close();
                    } catch (IOException ex) {
                        logger.error("Проблема при закрытии соединения с сервером", ex);
                    }
                }
            }).start();

            Scanner inputMessage = new Scanner(System.in);
            while (inputMessage.hasNext()) {
                outputStream.println(inputMessage.nextLine());
                outputStream.flush();
            }
//            Scanner inputStream = new Scanner(server.getInputStream());
//            while (inputStream.hasNext()) {
//                String text = inputStream.nextLine();
//                logger.info(text);
//            }
//            inputStream.close();
            outputStream.close();
        } catch (IOException e) {
            logger.error("Ошибка при подключении к серверу", e);
        }

    }
}