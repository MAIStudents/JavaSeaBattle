package ru.mai.lessons.rpks.Components;

import java.util.Objects;


public class InitPoints {
    private int x;
    private int y;

    public InitPoints(int x, int y) {
        this.x = x;
        this.y = y;
    }


    public int getY() {
        return y;
    }

    public int getX() {
        return x;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof InitPoints)) return false;

        InitPoints otherPoint = (InitPoints) other;

        return this.x == otherPoint.x && this.y == otherPoint.y;
    }

    @Override
    public String toString() {
        return "IntPoint: { " + x + " " + y + " }";
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}

