package ru.mai.lessons.rpks.utils;

public class Point {
    public boolean getHasShip() {
        return hasShip;
    }

    public void setHasShip(boolean hasShip) {
        this.hasShip = hasShip;
    }

    public boolean getIsHurt() {
        return isHurt;
    }

    public void setHurt(boolean hurt) {
        isHurt = hurt;
    }

    public Point(boolean hasShip) {
        this.hasShip = hasShip;
    }

    private boolean hasShip = false;
    private boolean isHurt = false;
}
