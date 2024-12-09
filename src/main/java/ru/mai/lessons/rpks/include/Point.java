package ru.mai.lessons.rpks.include;

public class Point {
    public boolean isTaken = false;
    public boolean isAlive = false;
    private int x;
    private int y;

    public Point() {

    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }
}
