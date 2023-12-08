package ru.mai.lessons.rpks.clients;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import org.apache.log4j.Logger;

import javafx.fxml.Initializable;

import java.io.IOException;
import java.net.URL;
import java.util.*;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import ru.mai.lessons.rpks.exceptions.WrongFieldFillingException;

// TODO: handle server connection error

public class ClientController implements Initializable {
    private static final Logger logger = Logger.getLogger(ClientController.class.getName());

    @FXML
    private Label labelSystemMessage;

    @FXML
    private Label labelAdditionalInfo;

    @FXML
    private HBox formBeginningArrangeShips;

    @FXML
    private GridPane gridShipsNumber;

    @FXML
    private HBox formGame;

    @FXML
    private Label labelTurn;

    @FXML
    private GridPane gridClientField;

    @FXML
    private GridPane gridEnemyField;

    @FXML
    private Button btnReady;

    private Client client;

    @FunctionalInterface
    private interface CreatorShipImageView {
        ImageView create();
    }

    private Node selectedShipFRomMyField = null;

    private Label myFieldCell;

    private final Map<GridPane, Owner> gridAndItsOwner = new HashMap<>();

    private Image shipImage;
    private Image shipAttackedImage;
    private Image missImage;

    private final Map<PointType, CreatorShipImageView> pointTypeAndItsImageViewCreator = new HashMap<>();
    private final String shipImageUrl = "ship.png";
    private final String shipAttackedImageUrl = "shipAttacked.png";
    private final String missImageUrl = "miss.png";
    private final int shipCellSize = 40, shipInfoSize = 30, sizeOfAttackedShipImg = 40, sizeOfShipImg = 40, sizeOfMissImg = 40;

    private boolean shipFillingIsStarted;
    private boolean shipFillingIsEnded;

    private PlayingField playingField;

    public ClientController() {
    }
    // todo: delete later, for debug

    // TODO: make a nice enter of ships
    // TODO: make a handle for user's turns and actions

    public void beginGame() {
        setFormGameVisible();
        // todo: initialize enemy turn, set on action grid
        // play game
    }

    public void markMissedTurn(Message message) {

    }

    public void markHitTurn(Message message) {

    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            uploadImages();
        } catch (Exception e) {
            System.err.println("No images!");
        }
        // todo: delete later
        shipFillingIsStarted = true;
        shipFillingIsEnded = false;

        formBeginningArrangeShips.setVisible(true);
        formGame.setVisible(false);

        labelSystemMessage.setText("Расстановка кораблей!");
        labelAdditionalInfo.setText("");

        initMyFieldGridAndShipGrid();

        // todo: may be move playing field to client later
        playingField = new PlayingField();
        updateShips(playingField.getInfoAboutShipsNeededToPut());

        activatePlayerFieldGrid();

        client = new Client();
        new Thread(() -> client.run());
    }

    private void uploadImages() {
        shipImage = new Image(getClass().getResourceAsStream(shipImageUrl));
        shipAttackedImage = new Image(getClass().getResourceAsStream(shipAttackedImageUrl));
        missImage = new Image(getClass().getResourceAsStream(missImageUrl));

        pointTypeAndItsImageViewCreator.put(PointType.DESTROYED, () -> {
            ImageView imageView = new ImageView(shipAttackedImage);
            imageView.setFitWidth(sizeOfAttackedShipImg);
            imageView.setFitHeight(sizeOfAttackedShipImg);
            return imageView;
        });

        pointTypeAndItsImageViewCreator.put(PointType.SHIP, () -> {
            ImageView imageView = new ImageView(shipImage);
            imageView.setFitWidth(sizeOfShipImg);
            imageView.setFitHeight(sizeOfShipImg);
            return imageView;
        });

        pointTypeAndItsImageViewCreator.put(PointType.MISS, () -> {
            ImageView imageView = new ImageView(missImage);
            imageView.setFitWidth(sizeOfMissImg);
            imageView.setFitHeight(sizeOfMissImg);
            return imageView;
        });
    }

    private Label createCell(int length) {
        Label label = new Label();
        label.setPrefSize(length, length);
        label.setAlignment(Pos.CENTER);
        return label;
    }

    private void initMyFieldGridAndShipGrid() {
        int rows = gridClientField.getRowCount(), columns = gridClientField.getColumnCount();
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                myFieldCell = createCell(shipCellSize);
                gridClientField.add(myFieldCell, i, j);
                gridEnemyField.add(createCell(shipCellSize), i, j);
            }
        }
        gridAndItsOwner.put(gridClientField, Owner.PLAYER);
        gridAndItsOwner.put(gridEnemyField, Owner.OPPONENT);

        rows = gridShipsNumber.getRowCount();
        columns = gridShipsNumber.getColumnCount();

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                gridShipsNumber.add(createCell(shipInfoSize), j, i);
            }
        }
    }

    public void clickedBtnReady() {
        if (shipFillingIsEnded) {
            labelSystemMessage.setText("Ожидаем готовности противника");
            try {
                client.writeToServer(new Message(client.getClientId(), Message.MessageType.SET_READY));
            } catch (IOException e) {
                // todo: log
                throw new RuntimeException(e);
            }
        } else {
            labelAdditionalInfoSetErrorMessage("Ещё не все корабли расставлены");
        }
    }

    private void activatePlayerFieldGrid() {
        gridClientField.setOnMouseClicked(e -> {
            if (!(e.getTarget() instanceof Label)) {
                return;
            }

            Label tartNode = (Label) e.getTarget();
            String style = "-fx-background-color: #00587A";
            labelAdditionalInfoSetInfoMessage("Выберите конечную клетку для корабля");
            if (selectedShipFRomMyField == null) {
                tartNode.setStyle(style);
                selectedShipFRomMyField = tartNode;
            } else {
                int startColumn = GridPane.getColumnIndex(selectedShipFRomMyField);
                int startRow = GridPane.getRowIndex(selectedShipFRomMyField);

                int finishColumn = GridPane.getColumnIndex(tartNode);
                int finishRow = GridPane.getRowIndex(tartNode);

                if (shipFillingIsStarted) {
                    List<Point> puttedPoints;
                    try {
                        puttedPoints = playingField.fillField(new Point(startRow, startColumn), new Point(finishRow, finishColumn));
                        for (Point puttedPoint : puttedPoints) {
                            updateShipCell(new Message(client.getClientId(), Message.MessageType.SHIP));
                        }

                        updateShips(playingField.getInfoAboutShipsNeededToPut());
                        labelAdditionalInfoSetSuccessMessage("Хорошее место =)");
                        if (playingField.allShipsAreFilled()) {
                            shipFillingIsEnded = true;
                            // todo: change system label
                            labelAdditionalInfoSetSuccessMessage("Все корабли расставлены!");
                            gridClientField.setOnMouseClicked(null);
                        }
                    } catch (WrongFieldFillingException ex) {
                        labelAdditionalInfoSetErrorMessage(ex.getMessage());
//                        throw new RuntimeException(ex);
                    }
                    // say to fill specific ship
//                    System.out.printf("Sent to server %d %d %d %d%n", startRow, startColumn, finishRow, finishColumn);
//                    try {
//                        phone.send(new FieldFillingDto(new Point(startRow, startColumn), new Point(finishRow, finishColumn)));
//                    } catch (IOException ex) {
//                        mainTextField.setText("Error. Can't sent points");
//                    }
                }
                selectedShipFRomMyField.setStyle(null);
                selectedShipFRomMyField = null;
            }
        });
    }


    private void updateShipCell(Message message) {
        GridPane fieldGrid = (message.getClientID() == client.getClientId() ? gridClientField : gridEnemyField);
        Label cellToUpdate = (Label) getFromGrid(fieldGrid, message.getPoint().row, message.getPoint().column);
        CreatorShipImageView creatorShipImageView = null;
        if (cellToUpdate != null) {
            switch (message.getMessageType()) {
                case HIT -> {
                    creatorShipImageView = pointTypeAndItsImageViewCreator.get(PointType.DESTROYED);
                }
                case MISSED -> {
                    creatorShipImageView = pointTypeAndItsImageViewCreator.get(PointType.MISS);
                }
                case SHIP -> {
                    creatorShipImageView = pointTypeAndItsImageViewCreator.get(PointType.SHIP);
                }
            }
            if (creatorShipImageView == null) return;
            CreatorShipImageView finalCreatorShipImageView = creatorShipImageView;
            Platform.runLater(() -> cellToUpdate.setGraphic(finalCreatorShipImageView.create()));
        }
    }

    private void updateShips(Map<Integer, Integer> shipInfo) {
        Set<Integer> currentNumberOfShipsAndItsCellValues = shipInfo.keySet();
        int column = 1;
        for (Integer shipCellValue : currentNumberOfShipsAndItsCellValues) {
            Label numberLabel = (Label) getFromGrid(gridShipsNumber, 0, column);
            Label valueLabel = (Label) getFromGrid(gridShipsNumber, 1, column);

            if (numberLabel == null || valueLabel == null) return;

            Platform.runLater(() -> {
                numberLabel.setText(String.valueOf(shipCellValue));
                valueLabel.setText(String.valueOf(shipInfo.get(shipCellValue)));
            });

            column++;
        }
    }

    private Node getFromGrid(GridPane gridPane, int row, int column) {
        for (Node node : gridPane.getChildren()) {
            if (GridPane.getColumnIndex(node) != null
                    && GridPane.getColumnIndex(node) != null
                    && GridPane.getRowIndex(node) == row
                    && GridPane.getColumnIndex(node) == column) {
                return node;
            }
        }
        return null;
    }

    public void disactivateOpponentField() {
        gridEnemyField.setOnMouseClicked(null);
    }

    public void activateOpponentField() {
        gridEnemyField.setOnMouseClicked(e -> {
            if (!(e.getTarget() instanceof Label)) {
                return;
            }

            Label pressedNode = (Label) e.getTarget();

            int column = GridPane.getColumnIndex(pressedNode);
            int row = GridPane.getRowIndex(pressedNode);

            try {
                client.writeToServer(new Message(client.getClientId(), Message.MessageType.TURN_INFO));
            } catch (IOException ex) {
                labelSystemMessage.setText("Error. Can't send point info");
                throw new RuntimeException(ex);
            }

//            System.out.printf("Sent to server %d %d\n", row, column);
//            try {
//                phone.send(new PointDto(new Point(row, column)));
//            } catch (IOException ex) {
//                labelSystemMessage.setText("Error. Can't sent point");
//            }
        });
    }

    public void toSetOnClose() {
        client.clientCloseConnection();
    }

    public void labelAdditionalInfoSetErrorMessage(String message) {
        labelAdditionalInfo.setText(message);
        labelAdditionalInfo.setStyle("-fx-text-fill: #E52B50");
    }

    public void labelAdditionalInfoSetInfoMessage(String message) {
        labelAdditionalInfo.setText(message);
        labelAdditionalInfo.setStyle("-fx-text-fill: black");
    }

    public void labelAdditionalInfoSetSuccessMessage(String message) {
        labelAdditionalInfo.setText(message);
        labelAdditionalInfo.setStyle("-fx-text-fill: #3B7A57");
    }

    public void labelTurnSetMyTurn() {
        labelTurn.setText("Ваш ход");
        labelTurn.setStyle("-fx-text-fill: #3B7A57");
    }

    public void labelTurnSetEnemyTurn() {
        labelTurn.setText("Ход противника");
        labelTurn.setStyle("-fx-text-fill: black");
    }

    public void setFormGameVisible() {
        formBeginningArrangeShips.setVisible(false);
        formGame.setVisible(true);
    }
}
