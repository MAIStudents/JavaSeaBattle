package ru.mai.lessons.rpks.clients;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import org.apache.log4j.Logger;

import javafx.fxml.Initializable;

import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import ru.mai.lessons.rpks.exceptions.WrongAttackArgumentException;
import ru.mai.lessons.rpks.exceptions.WrongFieldFillingException;

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

    private Image shipImage;
    private Image shipAttackedImage;
    private Image missImage;
    private Image seaImage;

    private final Map<Point.PointType, CreatorShipImageView> pointTypeAndItsImageViewCreator = new HashMap<>();
    private final String shipImageUrl = "ship.png";
    private final String shipAttackedImageUrl = "shipAttacked.png";
    private final String missImageUrl = "miss.png";
    private final String seaImageUrl = "sea.png";
    private final int shipCellSize = 40, shipInfoSize = 30, sizeOfAttackedShipImg = 40, sizeOfShipImg = 40, sizeOfMissImg = 40, sizeOfSeaImage = 40;

    private boolean shipFillingIsStarted;
    private boolean shipFillingIsEnded;

    private PlayingField playingField;
    ExecutorService executorService;

    public void beginGame() {
        labelSystemMessage.setText("Игра началась!");
        setFormGameVisible();
        btnReady.setDisable(true);
        btnReady.setVisible(false);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            uploadImages();
        } catch (Exception e) {
            logger.error("Ошибка загрузки изображений для игры!");
        }
        formBeginningArrangeShips.setVisible(true);
        formGame.setVisible(false);

        labelSystemMessage.setText("Ожидаем подключения противника");
        labelAdditionalInfo.setText("");

        playingField = new PlayingField();
        initMyShipGrid();
        updateShips(playingField.getInfoAboutShipsNeededToPut());

        activatePlayerFieldGrid();

        client = new Client(this);
        executorService = Executors.newFixedThreadPool(1);
        executorService.execute(client);
    }

    private void uploadImages() {
        shipImage = new Image(getClass().getResourceAsStream(shipImageUrl));
        shipAttackedImage = new Image(getClass().getResourceAsStream(shipAttackedImageUrl));
        missImage = new Image(getClass().getResourceAsStream(missImageUrl));
        seaImage = new Image(getClass().getResourceAsStream(seaImageUrl));

        pointTypeAndItsImageViewCreator.put(Point.PointType.DESTROYED, () -> {
            ImageView imageView = new ImageView(shipAttackedImage);
            imageView.setFitWidth(sizeOfAttackedShipImg);
            imageView.setFitHeight(sizeOfAttackedShipImg);
            return imageView;
        });

        pointTypeAndItsImageViewCreator.put(Point.PointType.SHIP, () -> {
            ImageView imageView = new ImageView(shipImage);
            imageView.setFitWidth(sizeOfShipImg);
            imageView.setFitHeight(sizeOfShipImg);
            return imageView;
        });

        pointTypeAndItsImageViewCreator.put(Point.PointType.MISS, () -> {
            ImageView imageView = new ImageView(missImage);
            imageView.setFitWidth(sizeOfMissImg);
            imageView.setFitHeight(sizeOfMissImg);
            return imageView;
        });

        pointTypeAndItsImageViewCreator.put(Point.PointType.BLANK, () -> {
            ImageView imageView = new ImageView(seaImage);
            imageView.setFitWidth(sizeOfSeaImage);
            imageView.setFitHeight(sizeOfSeaImage);
            return imageView;
        });
    }

    private Label createCell(int length) {
        Label label = new Label();
        label.setPrefSize(length, length);
        label.setAlignment(Pos.CENTER);
        return label;
    }

    private void initMyShipGrid() {
        int rows = gridShipsNumber.getRowCount();
        int columns = gridShipsNumber.getColumnCount();

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                gridShipsNumber.add(createCell(shipInfoSize), j, i);
            }
        }
    }

    private void initMyFieldGrid() {
        int rows = gridClientField.getRowCount(), columns = gridClientField.getColumnCount();
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                myFieldCell = createCell(shipCellSize);
                gridClientField.add(myFieldCell, i, j);
                gridEnemyField.add(createCell(shipCellSize), i, j);
            }
        }
    }

    private void setSea() {
        int rows = gridClientField.getRowCount(), columns = gridClientField.getColumnCount();
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                setBlankImage(gridClientField, i, j);
                setBlankImage(gridEnemyField, i, j);
            }
        }
    }

    private void setBlankImage(GridPane grid, int row, int col) {
        Label cellToUpdate = (Label) getFromGrid(grid, row, col);
        if (cellToUpdate != null) {
            CreatorShipImageView creatorShipImageView = pointTypeAndItsImageViewCreator.get(Point.PointType.BLANK);
            if (creatorShipImageView == null) return;
            cellToUpdate.setGraphic(creatorShipImageView.create());
        }
    }

    public void clickedBtnReady() {
        if (shipFillingIsEnded) {
            labelSystemMessage.setText("Ожидаем готовности противника");
            client.writeToServer(new Message(client.getClientId(), Message.MessageType.SET_READY));
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
                        puttedPoints = playingField.fillField(
                                new Point(startRow, startColumn),
                                new Point(finishRow, finishColumn));
                        for (Point puttedPoint : puttedPoints) {
                            updateShipCell(new TurnInfo(client.getClientId(),
                                    new Point(puttedPoint.row, puttedPoint.column), TurnInfo.TurnType.SHIP));
                        }

                        updateShips(playingField.getInfoAboutShipsNeededToPut());
                        labelAdditionalInfoSetSuccessMessage("Хорошее место =)");
                        if (playingField.allShipsAreFilled()) {
                            shipFillingIsEnded = true;
                            labelAdditionalInfoSetSuccessMessage("Все корабли расставлены!");
                            gridClientField.setOnMouseClicked(null);
                        }
                    } catch (WrongFieldFillingException ex) {
                        labelAdditionalInfoSetErrorMessage(ex.getMessage());
                    }
                }
                selectedShipFRomMyField.setStyle(null);
                selectedShipFRomMyField = null;
            }
        });
    }

    public void updateShipCell(TurnInfo turnInfo) {
        GridPane fieldGrid = (turnInfo.getClientID() == client.getClientId() ? gridClientField : gridEnemyField);
        Label cellToUpdate = (Label) getFromGrid(fieldGrid, turnInfo.getPoint().row, turnInfo.getPoint().column);
        CreatorShipImageView creatorShipImageView = null;
        if (cellToUpdate != null) {
            switch (turnInfo.getType()) {
                case HIT -> creatorShipImageView = pointTypeAndItsImageViewCreator.get(Point.PointType.DESTROYED);
                case MISS -> creatorShipImageView = pointTypeAndItsImageViewCreator.get(Point.PointType.MISS);
                case SHIP -> creatorShipImageView = pointTypeAndItsImageViewCreator.get(Point.PointType.SHIP);
            }
            if (creatorShipImageView == null) return;
            CreatorShipImageView finalCreatorShipImageView = creatorShipImageView;
            cellToUpdate.setGraphic(creatorShipImageView.create());
//            Platform.runLater(() -> cellToUpdate.setGraphic(finalCreatorShipImageView.create()));
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
            if (!(e.getTarget() instanceof Label pressedNode)) {
                return;
            }

            int column = GridPane.getColumnIndex(pressedNode);
            int row = GridPane.getRowIndex(pressedNode);

            client.writeToServer(
                    new TurnInfo(client.getClientId(), new Point(row, column), TurnInfo.TurnType.ATTACK));
        });
    }

    public TurnInfo getResponseToEnemyAttack(TurnInfo turnInfo) {
        boolean attackHit;
        try {
            attackHit = playingField.attackIsSuccess(turnInfo.getPoint());
        } catch (WrongAttackArgumentException e) {
            return new TurnInfo(client.getClientId(), turnInfo.getPoint(), TurnInfo.TurnType.WRONG);
        }
        if (attackHit) {
            updateShipCell(new TurnInfo(client.getClientId(), turnInfo.getPoint(), TurnInfo.TurnType.HIT));
            // get starting and ending points of sunken ship
            List<Point> sunkenShipPoints = playingField.didShipSunk(turnInfo.getPoint());
            if (sunkenShipPoints != null) {
                List<Point> neighborCells = getNeighborsCellsOfSunkenShip(sunkenShipPoints);
                markNeighborsOfMySunkenShip(neighborCells);
                return new TurnInfo(client.getClientId(), turnInfo.getPoint(), neighborCells, TurnInfo.TurnType.SUNKEN);
            }
            return new TurnInfo(client.getClientId(), turnInfo.getPoint(), TurnInfo.TurnType.HIT);
        }
        updateShipCell(new TurnInfo(client.getClientId(), turnInfo.getPoint(), TurnInfo.TurnType.MISS));
        return new TurnInfo(client.getClientId(), turnInfo.getPoint(), TurnInfo.TurnType.MISS);
    }

    public boolean allShipsAreDestroyed() {
        return playingField.allShipsAreDestroyed();
    }

    private List<Point> getNeighborsCellsOfSunkenShip(List<Point> sunkenShipStartingAndEndingPoints) {
        List<Point> neighborsCells = new ArrayList<>();
        Point startPoint = sunkenShipStartingAndEndingPoints.get(0);
        Point endPoint = sunkenShipStartingAndEndingPoints.get(1);
        boolean startRowIsFirst = startPoint.row == 0, startColIsFirst = startPoint.column == 0;
        boolean endRowIsLast = endPoint.row == 9, endColIsLast = endPoint.column == 9;
        int startRow = startPoint.row;
        int startCol = startColIsFirst ? startPoint.column : startPoint.column - 1;
        int endRow = endPoint.row;
        int endCol = endColIsLast ? endPoint.column : endPoint.column + 1;

        for (int i = startCol; i < endCol + 1; i++) {
            if (!startRowIsFirst) {
                neighborsCells.add(new Point(startRow - 1, i));
            }
            if (!endRowIsLast) {
                neighborsCells.add(new Point(endRow + 1, i));
            }
        }
        for (int i = startRow; i < endRow + 1; i++) {
            if (!startColIsFirst) {
                neighborsCells.add(new Point(i, startPoint.column - 1));
            }
            if (!endColIsLast) {
                neighborsCells.add(new Point(i, endPoint.column + 1));
            }
        }
        return neighborsCells;
    }

    private void markNeighborsOfMySunkenShip(List<Point> sunkenShipNeighborPoints) {
        for (Point neighbor : sunkenShipNeighborPoints) {
            updateShipCell(new TurnInfo(client.getClientId(), neighbor, TurnInfo.TurnType.MISS));
            playingField.setCellNextToSunkenShipMissed(neighbor);
        }
    }

    public void markNeighborsOfEnemySunkenShip(List<Point> sunkenShipNeighborPoints) {
        for (Point neighbor : sunkenShipNeighborPoints) {
            updateShipCell(new TurnInfo(client.getOpponentID(), neighbor, TurnInfo.TurnType.MISS));
        }
    }

    public void toSetOnClose() {
        client.clientCloseConnection();
        executorService.shutdown();

        try {
            if (!executorService.awaitTermination(1000, TimeUnit.MICROSECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
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

    public void setMyTurn() {
        labelTurn.setText("Ваш ход");
        labelTurn.setStyle("-fx-text-fill: #3B7A57");
        labelAdditionalInfoSetInfoMessage("Выберите клетку для атаки на поле противника");
        activateOpponentField();
    }

    public void setEnemyTurn() {
        labelTurn.setText("Ход противника");
        labelTurn.setStyle("-fx-text-fill: black");
        labelAdditionalInfoSetInfoMessage("Ожидайте хода противника");
        disactivateOpponentField();
    }

    public void setWin() {
        disactivateOpponentField();
        labelSystemMessage.setText("Игра окончена");
        labelTurn.setText("Вы выиграли! =)");
        labelAdditionalInfo.setVisible(false);
    }

    public void setDefeat() {
        disactivateOpponentField();
        labelSystemMessage.setText("Игра окончена");
        labelTurn.setText("Вы проиграли! =(");
        labelAdditionalInfo.setVisible(false);
    }

    public void setFormGameVisible() {
        formBeginningArrangeShips.setVisible(false);
        formGame.setVisible(true);
    }

    public void setShipFillingIsStarted() {
        this.shipFillingIsStarted = true;
        this.shipFillingIsEnded = false;
        labelSystemMessage.setText("Расстановка кораблей!");
        initMyFieldGrid();
        setSea();
    }

    public void stopTheGame() {
        gridClientField.setOnMouseClicked(null);
        gridEnemyField.setOnMouseClicked(null);
        labelSystemMessage.setText("Ваш враг струсил и отключился");
        labelAdditionalInfo.setText("");
        labelTurn.setText("Вы - победитель!");
        formBeginningArrangeShips.setVisible(false);
        formGame.setVisible(true);
        btnReady.setDisable(false);
    }
}
