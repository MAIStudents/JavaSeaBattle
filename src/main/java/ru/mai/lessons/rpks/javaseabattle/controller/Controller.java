package ru.mai.lessons.rpks.javaseabattle.controller;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import ru.mai.lessons.rpks.javaseabattle.Main;
import ru.mai.lessons.rpks.javaseabattle.controller.game_control_button_state.GameControlButtonState;
import ru.mai.lessons.rpks.javaseabattle.server_response_handler.ResponseHandler;
import ru.mai.lessons.rpks.javaseabattle.utils.Pair;

import java.io.IOException;
import java.net.Socket;

public class Controller {

    public static final String SHIP_CELL_TYPE = "-fx-background-color: #A3A3A3; -fx-border-color: #000000; -fx-opacity: 1;";

    public static final String SHOT_SHIP_CELL_TYPE = "-fx-background-color: #CD5C5C; -fx-border-color: #000000; -fx-opacity: 1;";

    public static final String MISSED_CELL_TYPE = "-fx-background-color: #064273; -fx-border-color: #000000; -fx-opacity: 1;";

    public static final String EMPTY_CELL_TYPE = "-fx-background-color: transparent; -fx-border-color: transparent;";

    public GameControlButtonState gameControlButtonState = GameControlButtonState.SET_SHIPS;

    private static final int[] SHIPS_CORRECT_COUNT = new int[] {0, 4, 3, 2, 1, 0, 0, 0, 0, 0, 0};

    private static final int[] shipsDefeated = new int[5];

    private Node[][] playerTableNodes;
    private Node[][] opponentTableNodes;

    public boolean canAttackOpponent = false;

    public ResponseHandler communication = null;

    @FXML
    private GridPane opponentTable;

    @FXML
    private GridPane playerTable;

    @FXML
    public Button gameControlButton;

    @FXML
    public Label hint;

    private String getTableCellId(GridPane table, int row, int column) {
        return table.getId() + "Cell" + row + column;
    }

    public Node getOpponentTableNode(int row, int column) {
        return opponentTableNodes[row][column];
    }

    public Node getPlayerTableNode(int row, int column) {
        return playerTableNodes[row][column];
    }

    public boolean markShipAsDefeated(int shipType) throws IOException {
        ++shipsDefeated[shipType];
        boolean isGameWon = true;
        for (int i = 0; i < shipsDefeated.length; ++i) {
            if (shipsDefeated[i] != SHIPS_CORRECT_COUNT[i]) {
                isGameWon = false;
                break;
            }
        }
        if (isGameWon) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(Main.BUNDLE.getString("alert.winMessageTitle"));
            alert.setHeaderText(Main.BUNDLE.getString("alert.winMessageText"));
            alert.show();
            opponentTable.setDisable(true);
            gameControlButton.setText(Main.BUNDLE.getString("win.gameControlButtonText"));
            communication.sendMessageToServer("WON");
            hint.setText(Main.BUNDLE.getString("win.winHint"));
            gameControlButtonState = GameControlButtonState.QUIT;
        }

        return isGameWon;
    }

    private void initializeTable(GridPane table) {
        final int rowsCount = table.getRowCount();
        final int columnsCount = table.getColumnCount();

        for (int row = 0; row < rowsCount; ++row) {
            for (int column = 0; column < columnsCount; ++column) {
                Label cell = new Label();
                cell.setStyle(EMPTY_CELL_TYPE);
                cell.setMaxWidth(Double.MAX_VALUE);
                cell.setMaxHeight(Double.MAX_VALUE);
                cell.setId(getTableCellId(table, row, column));
                int finalRow = row;
                int finalColumn = column;
                if (table.equals(playerTable)) {
                    cell.setOnMouseClicked(event -> handlePlayerTableCellClick(finalRow, finalColumn));
                    playerTableNodes[row][column] = cell;
                } else {
                    cell.setOnMouseClicked(event -> {
                        try {
                            handleOpponentTableCellClick(finalRow, finalColumn);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                    opponentTableNodes[row][column] = cell;
                }
                table.add(cell, column, row);
            }
        }
    }

    private boolean isShipCell(int row, int column) {
        if (row >= playerTable.getRowCount() || column >= playerTable.getColumnCount()
                || row < 0 || column < 0) {
            return false;
        }
        return playerTableNodes[row][column].getStyle().equals(SHIP_CELL_TYPE) || playerTableNodes[row][column].getStyle().equals(SHOT_SHIP_CELL_TYPE);
    }

    @FXML
    public void gameControlButton() throws IOException {
        switch (gameControlButtonState) {
            case SET_SHIPS -> {
                playerTable.setDisable(true);

                gameControlButton.setDisable(true);
                gameControlButton.setText(Main.BUNDLE.getString("wait.gameControlButtonText"));

                communication = new ResponseHandler(new Socket("localhost", 8080), this);
                new Thread(communication).start();

                opponentTable.setDisable(false);
                hint.setText(Main.BUNDLE.getString("wait.hintText"));

                gameControlButtonState = GameControlButtonState.PREVENT_CLOSING;
            }
            case QUIT_WITH_LOOSING -> {
                communication.sendMessageToServer("DISCONNECT_NOTIFIED");
                ((Stage) gameControlButton.getScene().getWindow()).close();
            }
            case QUIT -> {
                communication.sendMessageToServer("DISCONNECT");
                ((Stage) gameControlButton.getScene().getWindow()).close();
            }
        }
    }

    private void handlePlayerTableCellClick(int clickedCellRow, int clickedCellColumn) {
        if (playerTableNodes[clickedCellRow][clickedCellColumn].getStyle().equals(SHIP_CELL_TYPE)) {
            playerTableNodes[clickedCellRow][clickedCellColumn].setStyle(EMPTY_CELL_TYPE);
        } else {
            playerTableNodes[clickedCellRow][clickedCellColumn].setStyle(SHIP_CELL_TYPE);
        }

        for (int row = 0; row < playerTable.getRowCount(); ++row) {
            for (int column = 0; column < playerTable.getColumnCount(); ++column) {
                if (!isShipCell(row, column)) {
                    continue;
                }
                if (isShipCell(row - 1, column - 1) || isShipCell(row - 1, column + 1)
                        || isShipCell(row + 1, column - 1) || isShipCell(row + 1, column + 1)) {
                    gameControlButton.setDisable(true);
                    gameControlButton.setText(Main.BUNDLE.getString("wrongShipOrder.gameControlButtonText"));
                    return;
                }
                boolean isVerticalLine = isShipCell(row - 1, column) || isShipCell(row + 1, column);
                boolean isHorizontalLine = isShipCell(row, column + 1) || isShipCell(row, column - 1);
                if (isVerticalLine && isHorizontalLine) {
                    gameControlButton.setDisable(true);
                    gameControlButton.setText(Main.BUNDLE.getString("wrongShipOrder.gameControlButtonText"));
                    return;
                }
            }
        }

        int shipSize = 0;
        int[] actualShipsCount = new int[11];
        for (int row = 0; row < playerTable.getRowCount(); ++row) {
            for (int column = 0; column < playerTable.getColumnCount(); ++column) {
                if (!isShipCell(row, column)) {
                    if (shipSize != 0) {
                        ++actualShipsCount[shipSize];
                        shipSize = 0;
                    }
                    continue;
                }
                if (!isShipCell(row - 1, column) && !isShipCell(row + 1, column)) {
                    ++shipSize;
                }
            }
            if (shipSize != 0) {
                ++actualShipsCount[shipSize];
                shipSize = 0;
            }
        }

        for (int column = 0; column < playerTable.getColumnCount(); ++column) {
            for (int row = 0; row < playerTable.getRowCount(); ++row) {
                if (!isShipCell(row, column)) {
                    if (shipSize != 0) {
                        if (shipSize != 1) {
                            ++actualShipsCount[shipSize];
                        }
                        shipSize = 0;
                    }
                    continue;
                }
                if (!isShipCell(row, column - 1) && !isShipCell(row, column + 1)) {
                    ++shipSize;
                }
            }
            if (shipSize != 0) {
                if (shipSize != 1) {
                    ++actualShipsCount[shipSize];
                }
                shipSize = 0;
            }
        }

        for (int i = 0; i < SHIPS_CORRECT_COUNT.length; ++i) {
            if (actualShipsCount[i] != SHIPS_CORRECT_COUNT[i]) {
                gameControlButton.setDisable(true);
                gameControlButton.setText(Main.BUNDLE.getString("wrongShipOrder.gameControlButtonText"));
                return;
            }
        }

        gameControlButton.setDisable(false);
        gameControlButton.setText(Main.BUNDLE.getString("saveShipsLocation.gameControlButtonText"));
    }

    private void handleOpponentTableCellClick(int row, int column) throws IOException {
        if (!canAttackOpponent) {
            return;
        }
        if (opponentTableNodes[row][column].getStyle().equals(EMPTY_CELL_TYPE)) {
            communication.sendMessageToServer("SHOOT " + row + " " + column);
        } else {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(Main.BUNDLE.getString("alert.cellWasAlreadyCelectedTitle"));
            alert.setHeaderText(Main.BUNDLE.getString("alert.cellWasAlreadyCelectedText"));
            alert.show();
        }
    }

    @FXML
    public void initialize() {
        playerTableNodes = new Node[playerTable.getRowCount()][playerTable.getColumnCount()];
        opponentTableNodes = new Node[opponentTable.getRowCount()][opponentTable.getColumnCount()];

        opponentTable.setDisable(true);

        initializeTable(playerTable);
        initializeTable(opponentTable);
    }

    public Pair<Boolean, Integer> isShipSink(int row, int column) {
        boolean hasUnShotCell = false;

        int i = row, j = column;
        int shipLength = 1;
        while (isShipCell(i - 1, j) && !hasUnShotCell) {
            --i;
            ++shipLength;
            if (playerTableNodes[i][j].getStyle().equals(SHIP_CELL_TYPE)) {
                hasUnShotCell = true;
            }
        }
        i = row;
        while (isShipCell(i + 1, j) && !hasUnShotCell) {
            ++i;
            ++shipLength;
            if (playerTableNodes[i][j].getStyle().equals(SHIP_CELL_TYPE)) {
                hasUnShotCell = true;
            }
        }
        i = row;
        while (isShipCell(i, j - 1) && !hasUnShotCell) {
            --j;
            ++shipLength;
            if (playerTableNodes[i][j].getStyle().equals(SHIP_CELL_TYPE)) {
                hasUnShotCell = true;
            }
        }
        j = column;
        while (isShipCell(i, j + 1) && !hasUnShotCell) {
            ++j;
            ++shipLength;
            if (playerTableNodes[i][j].getStyle().equals(SHIP_CELL_TYPE)) {
                hasUnShotCell = true;
            }
        }

        return Pair.of(!hasUnShotCell, shipLength);
    }

}