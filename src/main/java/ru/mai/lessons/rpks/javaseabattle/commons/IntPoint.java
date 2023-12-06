package ru.mai.lessons.rpks.javaseabattle.commons;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class IntPoint {
    private int x;
    private int y;

    public IntPoint(int x, int y) {
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
        if (!(other instanceof IntPoint)) return false;

        IntPoint otherPoint = (IntPoint) other;

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

//    public static void main(String[] args) {
//        Map<IntPoint, Boolean> map = new HashMap<>();
//
//        IntPoint pointOne = new IntPoint(0, 0);
//
//        map.put(new IntPoint(0, 0), true);
//
//        for (Map.Entry<IntPoint, Boolean> p : map.entrySet()) {
//            System.out.println(p);
//        }
//
//        System.out.println(map.containsKey(new IntPoint(0, 0)));
//    }
}
