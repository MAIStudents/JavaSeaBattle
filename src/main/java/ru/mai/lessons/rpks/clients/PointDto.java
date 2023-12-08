package ru.mai.lessons.rpks.clients;

import java.io.Serializable;

public class PointDto implements Serializable {
    public Point point;
    public PointType pointType;
    public Owner pointOwner;

    public PointDto(Point point, PointType pointType, Owner pointOwner) {
        this.point = point;
        this.pointType = pointType;
        this.pointOwner = pointOwner;
    }

    public PointDto(Point point) {
        this.point = point;
    }
}
