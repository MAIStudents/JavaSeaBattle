package ru.mai.lessons.rpks.Game.Field;

public class Cell {
    private boolean isShip = false;
    private boolean isShot = false;
    private boolean isMiss = false;

    public boolean isShip() {
        return isShip;
    }

    public void setShip(boolean isShip) {
        this.isShip = isShip;
    }

    public boolean isShot() {
        return isShot;
    }

    public void setShot(boolean isShot) {
        this.isShot = isShot;
    }

    public boolean isMiss() {
        return isMiss;
    }

    public void setMiss(boolean miss) {
        isMiss = miss;
    }
}
