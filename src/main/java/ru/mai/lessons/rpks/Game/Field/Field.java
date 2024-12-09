package ru.mai.lessons.rpks.Game.Field;

public class Field {
    private final int SIZE = 10;
    private final Cell[][] field = new Cell[SIZE][SIZE];

    public static final int ONE_DECK_SHIPS = 4;
    public static final int TWO_DECK_SHIPS = 3;
    public static final int THREE_DECK_SHIPS = 2;
    public static final int FOUR_DECK_SHIPS = 1;

    public int currentOneDeckShips = ONE_DECK_SHIPS;
    public int currentTwoDeckShips = TWO_DECK_SHIPS;
    public int currentThreeDeckShips = THREE_DECK_SHIPS;
    public int currentFourDeckShips = FOUR_DECK_SHIPS;

    private int remainingShips = getMaximumParts();

    public Field() {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                field[i][j] = new Cell();
            }
        }
    }

    public Cell getCell(int x, int y) {
        return field[x][y];
    }

    public int getSize() {
        return SIZE;
    }

    private int getMaximumParts() {
        return ONE_DECK_SHIPS + 2 * TWO_DECK_SHIPS + 3 * THREE_DECK_SHIPS + 4 * FOUR_DECK_SHIPS;
    }

    public boolean placePart(int x, int y) {
        if (isFilled()) {
            return false;
        } else if (field[x][y].isShip()) {
            return false;
        } else if (checkCellsAround(x, y)) {
            field[x][y].setShip(true);
            return true;
        }
        return false;
    }

    public boolean isFilled() {
        return currentOneDeckShips == 0 && currentTwoDeckShips == 0 && currentThreeDeckShips == 0 && currentFourDeckShips == 0;
    }

    private boolean checkCellsAround(int x, int y) {
        boolean diagonal = checkDiagonal(x, y);
        if (!diagonal) {
            return false;
        }

        int leftHorizontal = checkHorizontalLeft(x, y);
        int rightHorizontal = checkHorizontalRight(x, y);
        int upVertical = checkVerticalUp(x, y);
        int downVertical = checkVerticalDown(x, y);

        int horizontal = leftHorizontal + rightHorizontal;
        int vertical = upVertical + downVertical;
        if (vertical > 0 && horizontal > 0) {
            return false;
        }
        if (vertical == 0 && horizontal == 0) {
            return updateShips(1);
        }
        if (vertical > 0) {
            boolean result = updateShips(vertical + 1);
            if (result) {
                destroyShip(upVertical);
                destroyShip(downVertical);
            }
            return result;
        }
        boolean result = updateShips(horizontal + 1);
        if (result) {
            destroyShip(leftHorizontal);
            destroyShip(rightHorizontal);
        }
        return result;
    }

    private void destroyShip(int size) {
        switch (size) {
            case 1:
                ++currentOneDeckShips;
                break;
            case 2:
                ++currentTwoDeckShips;
                break;
            case 3:
                ++currentThreeDeckShips;
                break;
            case 4:
                ++currentFourDeckShips;
                break;
            default:
                break;
        }
    }

    private boolean updateShips(int size) {
        if (size == 4 && currentFourDeckShips > 0) {
            --currentFourDeckShips;
            return true;
        }
        if (size == 3 && (currentThreeDeckShips > 0 || currentFourDeckShips > 0)) {
            --currentThreeDeckShips;
            return true;
        }
        if (size == 2 && (currentTwoDeckShips > 0 || currentThreeDeckShips > 0 || currentFourDeckShips > 0)) {
            --currentTwoDeckShips;
            return true;
        }
        if (size == 1 && (currentOneDeckShips > 0 || currentTwoDeckShips > 0 || currentThreeDeckShips > 0 || currentFourDeckShips > 0)) {
            --currentOneDeckShips;
            return true;
        }
        return false;
    }

    private int checkHorizontalRight(int x, int y) {
        int result = 0;
        for (int i = x + 1; i < SIZE && field[i][y].isShip(); ++i) {
            ++result;
        }
        return result;
    }

    private int checkHorizontalLeft(int x, int y) {
        int result = 0;
        for (int i = x - 1; i >= 0 && field[i][y].isShip(); --i) {
            ++result;
        }
        return result;
    }

    private int checkVerticalUp(int x, int y) {
        int result = 0;
        for (int i = y + 1; i < SIZE && field[x][i].isShip(); ++i) {
            ++result;
        }
        return result;
    }

    private int checkVerticalDown(int x, int y) {
        int result = 0;
        for (int i = y - 1; i >= 0 && field[x][i].isShip(); --i) {
            ++result;
        }
        return result;
    }

    private boolean checkDiagonal(int x, int y) {
        if (x > 0 && y > 0 && field[x - 1][y - 1].isShip()) {
            return false;
        }
        if (x < SIZE - 1 && y < SIZE - 1 && field[x + 1][y + 1].isShip()) {
            return false;
        }
        if (y > 0 && x < SIZE - 1 && field[x + 1][y - 1].isShip()) {
            return false;
        }
        return !(y < SIZE - 1 && x > 0 && field[x - 1][y + 1].isShip());
    }

    public boolean hasRemainingShips() {
        return remainingShips > 0;
    }

    public boolean isShipAt(int x, int y) {
        return field[x][y].isShip();
    }

    public void setMiss(int x, int y) {
        field[x][y].setMiss(true);
    }

    public void setShot(int x, int y) {
        field[x][y].setShot(true);
        --remainingShips;
    }

    public void updateShipsSizes(int x, int y) {
        int horizontalRight = checkHorizontalRight(x, y);
        int horizontalLeft = checkHorizontalLeft(x, y);

        int verticalUp = checkVerticalUp(x, y);
        int verticalDown = checkVerticalDown(x, y);

        int horizontal = horizontalLeft + horizontalRight;
        int vertical = verticalDown + verticalUp;

        if (horizontal == 0 && vertical == 0) {
            destroyShip(1);
        } else if (horizontal > 0) {
            destroyShip(horizontal + 1);
            updateShips(horizontalLeft);
            updateShips(horizontalRight);
        } else {
            destroyShip(vertical + 1);
            updateShips(verticalUp);
            updateShips(verticalDown);
        }
    }
}
