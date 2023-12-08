package ru.mai.lessons.rpks.clients;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ru.mai.lessons.rpks.exceptions.WrongFieldFillingException;
import ru.mai.lessons.rpks.exceptions.WrongAttackArgumentException;

public class PlayingField {
    private enum Cell {
        SHIP_PART('S'), MISS('M'), STRICKEN_PART_OF_SHIP('X'), BLANK('_');
        final char symbol;

        Cell(char symbol) {
            this.symbol = symbol;
        }
    }

    private final int NUMBER_OF_CELLS_IN_ROW = 10;
    private final Cell[][] field = new Cell[NUMBER_OF_CELLS_IN_ROW][NUMBER_OF_CELLS_IN_ROW];
    private final Map<Integer, Integer> cellValueOfShipsAndTheirsNumbers;
    private final Map<Integer, Integer> currentCellValueOfShipsAndTheirsNumbers;
    private int NUMBER_OF_SHIP_PART_CELLS = 0;

    public PlayingField() {
        for (int i = 0; i < field.length; ++i) {
            for (int j = 0; j < field.length; ++j) {
                field[i][j] = Cell.BLANK;
            }
        }

        cellValueOfShipsAndTheirsNumbers = new HashMap<>();
        cellValueOfShipsAndTheirsNumbers.put(4, 1); NUMBER_OF_SHIP_PART_CELLS += 4 * 1; // корабль 4х палубный
        cellValueOfShipsAndTheirsNumbers.put(3, 2); NUMBER_OF_SHIP_PART_CELLS += 3 * 2;
        cellValueOfShipsAndTheirsNumbers.put(2, 3); NUMBER_OF_SHIP_PART_CELLS += 2 * 3;
        cellValueOfShipsAndTheirsNumbers.put(1, 4); NUMBER_OF_SHIP_PART_CELLS += 1 * 4;

        currentCellValueOfShipsAndTheirsNumbers = new HashMap<>();
        for (Integer shipsSize : cellValueOfShipsAndTheirsNumbers.keySet()) {
            currentCellValueOfShipsAndTheirsNumbers.put(shipsSize, 0);
        }
    }


    public boolean attackIsSuccess(Point point) throws WrongAttackArgumentException {
        if (!allShipsAreFilled()) {
            throw new WrongAttackArgumentException("Not all ships are filled");
        }
        if (NUMBER_OF_SHIP_PART_CELLS == 0) {
            throw new WrongAttackArgumentException("No ship cells to attack");
        }

        int row = point.row;
        int column = point.column;
        if (row < 0 || row >= NUMBER_OF_CELLS_IN_ROW || column < 0 || column >= NUMBER_OF_CELLS_IN_ROW) {
            throw new WrongAttackArgumentException(String.format("Wrong argument values: (%d, %d)", column, row));
        }
        Cell attackedCell = field[row][column];
        if (attackedCell.equals(Cell.MISS) || attackedCell.equals(Cell.STRICKEN_PART_OF_SHIP)) {
            throw new WrongAttackArgumentException("Cell is already attacked");
        }

        if (attackedCell.equals(Cell.SHIP_PART)) {
            field[row][column] = Cell.STRICKEN_PART_OF_SHIP;
            NUMBER_OF_SHIP_PART_CELLS--;
            return true;
        } else {
            field[row][column] = Cell.MISS;
            return false;
        }
    }


    public List<Point> fillField(Point startPoint, Point finishPoint) throws WrongFieldFillingException
    {
        throwExceptionIfFinishPointIsWrong(finishPoint, startPoint);

        int shipSize = startPoint.column == finishPoint.column
                ? Math.abs(startPoint.row - finishPoint.row) + 1
                : Math.abs(startPoint.column - finishPoint.column) + 1;
        if (!cellValueOfShipsAndTheirsNumbers.containsKey(shipSize)) {
            throw new WrongFieldFillingException("Согласно правилам, нет кораблей размером " + shipSize);
        }

        int currentNumberOfShips = currentCellValueOfShipsAndTheirsNumbers.get(shipSize);
        if (currentNumberOfShips == cellValueOfShipsAndTheirsNumbers.get(shipSize)) {
            throw new WrongFieldFillingException("Больше не расставить кораблей размером " + shipSize);
        }

        Point tmp = startPoint;
        List<Point> pointsToMakeShip = new ArrayList<>();
        while (!finishPoint.equals(tmp)) {
            if (pointHasPartOfShipNeighbour(tmp)) {
                throw new WrongFieldFillingException(String.format("Клтека (%d; %s) поставлена рядом с другим кораблем", tmp.row, tmp.column));
            }
            pointsToMakeShip.add(tmp);
            tmp = returnCloserPoint(tmp, finishPoint);
        }
        if (pointHasPartOfShipNeighbour(tmp)) {
            throw new WrongFieldFillingException(String.format("Клетка (%d; %s) поставлена рядом с другим кораблем", tmp.row, tmp.column));
        }
        pointsToMakeShip.add(tmp);

        for (Point point : pointsToMakeShip) {
            field[point.row][point.column] = Cell.SHIP_PART;
        }
        currentCellValueOfShipsAndTheirsNumbers.put(shipSize, currentNumberOfShips+1);
        return pointsToMakeShip;
    }
    private void throwExceptionIfFinishPointIsWrong(Point finishPoint, Point startPoint) throws WrongFieldFillingException
    {
        int rowOfFinishPoint = finishPoint.row;
        int columnOfFinishPoint = finishPoint.column;

        if (columnOfFinishPoint < 0 || columnOfFinishPoint >= NUMBER_OF_CELLS_IN_ROW
                || rowOfFinishPoint < 0 || rowOfFinishPoint >= NUMBER_OF_CELLS_IN_ROW) {
            throw new WrongFieldFillingException(String.format("Неправильно выбраны точки: (%d, %d)", rowOfFinishPoint, columnOfFinishPoint));
        }

        int differenceBetweenRows = rowOfFinishPoint - startPoint.row;
        int differenceBetweenColumns = columnOfFinishPoint - startPoint.column;
        if (differenceBetweenColumns != 0 && differenceBetweenRows != 0) {
            throw new WrongFieldFillingException("Клетки должны быть на одной линии!");
        }
    }
    private boolean pointHasPartOfShipNeighbour(Point point) {
        List<Point> neighbours = getNeighbours(point);
        for (Point neighbour : neighbours) {
            Cell currentCell = field[neighbour.row][neighbour.column];
            if (currentCell.equals(Cell.SHIP_PART)) {
                return true;
            }
        }
        return false;
    }
    private List<Point> getNeighbours(Point point) {
        int column = point.column;
        int row = point.row;

        List<Point> neighbours = new ArrayList<>();
        for (int i = row-1; i <= row+1; i++) {
            for (int j = column-1; j <= column+1; j++) {
                if (i >= 0 && j >= 0 && i < NUMBER_OF_CELLS_IN_ROW && j < NUMBER_OF_CELLS_IN_ROW && (i != row || j != column)) {
                    neighbours.add(new Point(i, j));
                }
            }
        }
        return neighbours;
    }
    private Point returnCloserPoint(Point currentPoint, Point destPoint) {
        int columnDifference = destPoint.column - currentPoint.column;
        int rowDifference = destPoint.row - currentPoint.row;

        int addToColumn = Integer.compare(columnDifference, 0);
        int addToRow = Integer.compare(rowDifference, 0);

        return new Point(currentPoint.row + addToRow, currentPoint.column + addToColumn);
    }

    public Map<Integer, Integer> getInfoAboutShipsNeededToPut() {
        Map<Integer, Integer> differenceBetweenCurrentAndMaxValueOfShips = new HashMap<>();
        currentCellValueOfShipsAndTheirsNumbers.keySet()
                .forEach((key) -> {
                    Integer difference = cellValueOfShipsAndTheirsNumbers.get(key) - currentCellValueOfShipsAndTheirsNumbers.get(key);
                    differenceBetweenCurrentAndMaxValueOfShips.put(key, difference);
                });
        return differenceBetweenCurrentAndMaxValueOfShips;
    }

    public Map<Integer, Integer> getInfoAboutShipsNeededToAttack() {
        return currentCellValueOfShipsAndTheirsNumbers;
    }

    public boolean allShipsAreFilled() {
        // TODO: REMOVE LATER. FOR DEBUG. DANGER!
        return true;
        /*
        for (Integer shipsSize : currentCellValueOfShipsAndTheirsNumbers.keySet()) {
            int currentNumberOfShips = currentCellValueOfShipsAndTheirsNumbers.get(shipsSize);
            int allNumberOfShips = cellValueOfShipsAndTheirsNumbers.get(shipsSize);

            if (currentNumberOfShips != allNumberOfShips) {
                return false;
            }
        }
        return true;*/
    }

    public boolean allShipsAreDestroyed() {
        return NUMBER_OF_SHIP_PART_CELLS == 0;
    }
}
