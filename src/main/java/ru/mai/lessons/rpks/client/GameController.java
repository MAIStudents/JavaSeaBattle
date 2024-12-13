package ru.mai.lessons.rpks.client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import ru.mai.lessons.rpks.logger.Logger;
import ru.mai.lessons.rpks.utils.Message;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GameController {

    private final static String missedStyle = "-fx-background-color: gray;";
    private final static String hurtStyle = "-fx-background-color: red;";
    private final static String shipStyle = "-fx-background-color: green;";

    private List<List<Button>> myButtonField;
    private List<List<Button>> enemyButtonField;
    private List<List<Boolean>> myBattlefieldModel;

    private Button myBtn;
    private Button enemyBtn;

    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 18088;
    private Socket clientSocket;
    private Thread listenThread;

    private BufferedReader in;
    private BufferedWriter out;

    private Logger logger = new Logger(getClass());



    private static enum ShipDirection {
        hor, ver, mult
    }


    public GameController() {
        myButtonField = new ArrayList<>(10);
        enemyButtonField = new ArrayList<>(10);

        myBattlefieldModel = new ArrayList<>(10);

        for(int i = 0; i < 10; ++i) {
            List<Boolean> tmp = new ArrayList<>(10);

            for (int j = 0; j < 10; ++j) {
                tmp.add(false);
            }

            myBattlefieldModel.add(tmp);
        }

        updateConnection();
    }

    private void updateThread() {
        if (isCorrect()) {
            listenThread = new Thread(() -> {
                try {
                    while (!Thread.interrupted() && !clientSocket.isClosed()) {
                        if (in.ready()) {
                            response();
                        } else {
                            Thread.currentThread().yield();
                        }
                    }
                } catch (IOException e) {
                    logger.error(e.getMessage());
                    e.printStackTrace();
                } finally {
                    logger.info("Game ended\n");
                }
            });

            listenThread.start();
        }
    }

    public void updateConnection() {
        closeConnections();
        try {
            clientSocket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
        } catch (IOException e) {
            logger.error(e.getMessage());
        }

        updateThread();
    }

    public void setMyBtn(Button myBtn) {
        this.myBtn = myBtn;
    }

    public void setEnemyBtn(Button enemyBtn) {
        this.enemyBtn = enemyBtn;
    }

    public boolean isCorrect() {
        return clientSocket != null && in != null && out != null;
    }

    public void clearFields() {
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                enemyButtonField.get(i).get(j).setDisable(true);
                enemyButtonField.get(i).get(j).setStyle("");
                myButtonField.get(j).get(i).setDisable(false);
                myButtonField.get(j).get(i).setStyle("");
                myBattlefieldModel .get(i).set(j, false);
                myBattlefieldModel.get(i).set(j,false);
            }
        }
    }

    public void addMyButtonRow(List<Button> row) {
        myButtonField.add(row);
    }

    public void addEnemyButtonRow(List<Button> row) {
        enemyButtonField.add(row);
    }

    public void addShipOnCell(int row, int column, Button cell) {
        if (canAddShip(row, column)) {
            myBattlefieldModel.get(row).set(column, true);
            cell.setStyle(shipStyle);
        }
    }

    public void closeConnections() {
        if (listenThread != null) {
            listenThread.interrupt();
            try {
                listenThread.join();
            } catch (InterruptedException e) {
                logger.info(e.getMessage());
            }
        }

        try {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            if (listenThread != null && listenThread.isAlive()) {
                listenThread.interrupt();
                listenThread.join();
            }
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException | InterruptedException e) {
            System.out.printf(e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    private boolean canAddShip(int row, int column) {
        if ((row > 0 && column > 0 && myBattlefieldModel.get(row - 1).get(column - 1)) ||
                (row > 0 && column < 9 && myBattlefieldModel.get(row - 1).get(column + 1)) ||
                (row < 9 && column > 0 && myBattlefieldModel.get(row + 1).get(column - 1)) ||
                (row < 9 && column < 9 && myBattlefieldModel.get(row + 1).get(column + 1))) {
            return false;
        }

        boolean up = row > 0 && myBattlefieldModel.get(row - 1).get(column);
        boolean down = row < 9 && myBattlefieldModel.get(row + 1).get(column);
        boolean left = column > 0 && myBattlefieldModel.get(row).get(column - 1);
        boolean right = column < 9 && myBattlefieldModel.get(row).get(column + 1);

        int counter = 0;

        if (up) {
            ++counter;
        }
        if (down) {
            ++counter;
        }
        if (left) {
            ++counter;
        }
        if (right) {
            ++counter;
        }

        if (counter > 1) {
            return false;
        }
        if (counter == 0) {
            return true;
        }

        int x = up ? row - 1 : down ? row + 1 : row;
        int y = left ? column - 1 : right ? column + 1 : column;

        ShipDirection dir = getShipDirection(x, y);

        if (dir == ShipDirection.mult)
            return true;
        else if (dir ==  ShipDirection.hor && x == row) {
            return true;
        } else return dir == ShipDirection.ver && y == column;
    }

    private ShipDirection getShipDirection(int x, int y) {
        boolean up = x > 0 && myBattlefieldModel.get(x - 1).get(y);
        boolean down = x < 9 && myBattlefieldModel.get(x + 1).get(y);
        boolean left = y > 0 && myBattlefieldModel.get(x).get(y - 1);
        boolean right = y < 9 && myBattlefieldModel.get(x).get(y + 1);

        if (up || down) {
            return ShipDirection.ver;
        } else if (left || right) {
            return ShipDirection.hor;
        } else {
            return ShipDirection.mult;
        }
    }

    public void removeShipFromCell(int row, int column, Button cell) {
        if (canRemoveShip(row, column)) {
            myBattlefieldModel.get(row).set(column, false);
            cell.setStyle("");
        }
    }

    private boolean canRemoveShip(int row, int column) {
        boolean up = row > 0 && myBattlefieldModel.get(row - 1).get(column);
        boolean down = row < 9 && myBattlefieldModel.get(row + 1).get(column);
        boolean left = column > 0 && myBattlefieldModel.get(row).get(column - 1);
        boolean right = column < 9 && myBattlefieldModel.get(row).get(column + 1);

        int counter = 0;

        if (up) {
            ++counter;
        }
        if (down) {
            ++counter;
        }
        if (left) {
            ++counter;
        }
        if (right) {
            ++counter;
        }

        return counter <= 1;
    }

    public boolean isCellCanBeAttacked(int x, int y) {
        String str = enemyButtonField.get(x).get(y).getStyle();
        return str.isEmpty();
    }

    public void makeMove(int x, int y) {

        try {
            out.write(new Message(Message.MessageType.attack, String.format("%d;%d", x, y)).toString());
            out.flush();
        } catch (IOException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }
    }

    public void disableEnemyField() {
        for (List<Button> lst : enemyButtonField) {
            for (Button btn : lst) {
                btn.setDisable(true);
            }
        }
    }

    private void disableMyField() {
        for (List<Button> lst : myButtonField) {
            for (Button btn : lst) {
                btn.setDisable(true);
            }
        }
    }

    private void enableEnemyField() {
        for (List<Button> lst : enemyButtonField) {
            for (Button btn : lst) {
                btn.setDisable(false);
            }
        }
    }

    private void enableMyField() {
        for (List<Button> lst : myButtonField) {
            for (Button btn : lst) {
                btn.setDisable(false);
            }
        }
    }

    private void response() {
        try {
            String str = in.readLine();
            Message message = Message.fromString(str);

            if (message.getType() != Message.MessageType.heart) {
                logger.info("Response for: {}\b", message.toString());
            }

            switch (message.getType()) {
                case heart -> {
                    out.write(message.toString());
                    out.flush();
                }
                case accept -> {
                    boolean good = message.getContent().equals("1");

                    if (good) {
                        Platform.runLater(() -> {myBtn.setText("Waiting other player");});
                    } else {
                        Platform.runLater(this::showProcessWrongShip);
                    }
                }
                case start -> {
                    Platform.runLater(() -> {
                        myBtn.setText("In game now");
                        myBtn.setDisable(true);
                    });
                }
                case attack -> {
                    if (message.getIsForMe()) {
                        Platform.runLater(() -> {
                            myBtn.setText("My turn");
                            enableEnemyField();
                        });
                    } else {
                        Platform.runLater(() -> {
                            myBtn.setText("Enemy`s turn");
                            disableEnemyField();
                        });
                    }
                }
                case hit -> {
                    Platform.runLater(() -> {
                        drawHit(message.getContent(), message.getIsForMe());
                    });
                }
                case win -> {
                    if (message.getIsForMe()) {
                        Platform.runLater(this::showVictory);
                    } else {
                        Platform.runLater(this::showDefeat);
                    }
                }
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }
    }

    private void drawHit(String message, boolean forMe) {
        List<List<Button>> field = forMe ? myButtonField : enemyButtonField;

        List<String> tokens = Arrays.asList(message.split(";"));

        if (tokens.size() % 3 != 0) {
            logger.error("Incorrect message length for drawHit");
            return;
        }

        for(int i = 0; i < tokens.size(); i += 3) {
            try {
                int x = Integer.parseInt(tokens.get(i));
                int y = Integer.parseInt(tokens.get(i + 1));
                int style = Integer.parseInt(tokens.get(i + 2));

                if (style == 1) {
                    Platform.runLater(() -> {
                        field.get(x).get(y).setStyle(missedStyle);
                    });
                } else {
                    Platform.runLater(() -> {
                        field.get(x).get(y).setStyle(hurtStyle);
                    });
                }

            } catch (NumberFormatException e) {
                logger.error(e.getMessage());
                e.printStackTrace();

                return;
            }
        }
    }

    private void showVictory() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Victory");
        alert.setContentText("You won battle!");
        alert.showAndWait();

        enemyBtn.getOnAction().handle(new ActionEvent());
    }

    private void showDefeat() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Defeat");
        alert.setContentText("All ships were flooded");
        alert.showAndWait();

        enemyBtn.getOnAction().handle(new ActionEvent());
    }

    private void showProcessWrongShip() {
        myBtn.setText("Are you ready?");
        myBtn.setDisable(false);
        enableMyField();

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Field error");
        alert.setContentText("""
                                You must have on field:
                                \t-1 four-deck ship
                                \t-2 three-deck ships
                                \t-3 two-deck ships
                                \t-4 one-deck ships
                                
                                You cannot place ships by diagonal
                                """);
        alert.showAndWait();
    }

    public void tryStartGame() {
        try {
            out.write(makeStartMessage().toString());
            out.flush();
        } catch (IOException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }

        disableEnemyField();
        disableMyField();

        myBtn.setDisable(true);

        myBtn.setText("Waiting server response...");
    }

    private Message makeStartMessage() {

        StringBuilder data = new StringBuilder();

        for(List<Boolean> lst : myBattlefieldModel) {
            for (boolean cell : lst) {
                data.append(cell ? 1 : 0);
            }
        }

        return new Message(Message.MessageType.start, data.toString());
    }
}
