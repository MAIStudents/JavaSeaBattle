package ru.mai.lessons.rpks.clients;

public class ClientOne {
    public static void main(String[] args) {
        Client client = new Client("localhost", 8843);
        client.start();
    }
}
