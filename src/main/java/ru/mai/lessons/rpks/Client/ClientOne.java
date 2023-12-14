package ru.mai.lessons.rpks.Client;

public class ClientOne {
    public static void main(String[] args) {
        Client client = new Client("127.0.0.1", 8843, "First");
        client.start();
    }
}