package ru.mai.lessons.rpks.clients;

import java.io.Serializable;
import java.util.List;

public class TurnInfo implements Serializable {
    Point point;
    List<Point> listPoint;
    int clientID;

    public enum TurnType {
        HIT, SUNKEN, MISS, WRONG, SHIP, ATTACK
    }

    TurnType type;

    TurnInfo(int clientID, Point point, TurnType type) {
        this.clientID = clientID;
        this.point = point;
        this.type = type;
    }

    TurnInfo(int clientID, Point point, List<Point> listPoint, TurnType type) {
        this.clientID = clientID;
        this.point = point;
        this.listPoint = listPoint;
        this.type = type;
    }

    @Override
    public String toString() {
        if (listPoint == null) {
            return "TurnInfo:{" + clientID + ", " + point + ", " + type + "}";
        }
        return "TurnInfo{" + clientID + ", " + point + ", " + listPoint + ", " + type + "}";
    }

    public int getClientID() {
        return clientID;
    }

    public Point getPoint() {
        return point;
    }

    public List<Point> getListPoint() {
        return listPoint;
    }

    public TurnType getType() {
        return type;
    }
}
