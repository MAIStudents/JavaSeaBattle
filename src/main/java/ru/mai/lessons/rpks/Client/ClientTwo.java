package ru.mai.lessons.rpks.Client;

public class ClientTwo {
    public static void main(String[] args) {
        Client client = new Client("127.0.0.1", 8843, "Second");
        client.start();
    }
}
