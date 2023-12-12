package ru.mai.lessons.rpks.javaseabattle.clients;

public class PlayerOne {
    public static void main(String[] args) {
        Client client = new Client("localhost", 8832, "First");
        client.start();
    }
}
