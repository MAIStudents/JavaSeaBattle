package ru.mai.lessons.rpks.javaseabattle.clients;

public class PlayerTwo {
    public static void main(String[] args) {
        Client client = new Client("localhost", 8832, "Second");
        client.start();
    }
}
