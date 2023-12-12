package ru.mai.lessons.rpks.javaseabattle.server;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

import static ru.mai.lessons.rpks.javaseabattle.commons.Responses.LEAVE;

public class ClientHandler implements Runnable {
    private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());

    private static final String ANCHOR_NAME = "###";

    private Socket client;
    private Server server;
    private String name;
    private String clientMessage;

    public ClientHandler(Socket client, Server server) {
        this.client = client;
        this.server = server;
    }

    @Override
    public void run() {

        boolean isPlayerOnline = true;
        Scanner inputStream = null;

        try {

            inputStream = new Scanner(client.getInputStream());

            while (inputStream.hasNext() && isPlayerOnline) {

                String text = inputStream.nextLine();

                if (text.equals(LEAVE)) {
                    isPlayerOnline = false;
                }

                logger.info("Message from client: " + text);

                if (text.contains(ANCHOR_NAME)) {
                    name = text.substring(text.indexOf(ANCHOR_NAME) + ANCHOR_NAME.length());
                } else {
                    clientMessage = text;
                }
            }

            clientMessage = LEAVE;
            server.removeClient(this);

        } catch (IOException e) {
            logger.error("Error processing client", e);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }

            if (client != null) {
                try {
                    client.close();
                } catch (IOException ex) {
                    logger.error("Client close error", ex);
                }
            }
        }
    }

    public void sendMessage(String message) {
        PrintWriter outputStream = null;
        if (client == null) {
            return;
        }

        try {
            outputStream = new PrintWriter(client.getOutputStream());
            outputStream.println(message);
            outputStream.flush();
        } catch (IOException e) {
            logger.error("Problem while writing message to client stream: " + client.toString(), e);
        }
    }

    public String getName() {
        return name;
    }

    public String getClientMessage() {

        while (clientMessage == null) {
            try {
                Thread.sleep(10L);
            } catch (InterruptedException e) {
                logger.error("Thread sleep getClientMessage " + name, e);
            }
        }

        String message = clientMessage;
        clientMessage = null;
        return message;
    }
}
