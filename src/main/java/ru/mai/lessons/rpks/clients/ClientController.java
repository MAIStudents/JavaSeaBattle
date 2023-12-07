package ru.mai.lessons.rpks.clients;

import javafx.scene.control.Button;
import org.apache.log4j.Logger;

import javafx.fxml.Initializable;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

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

    public ClientController(Client client) {
        this.client = client;
    }

    // TODO: make a nice enter of ships
    // TODO: make a handle for user's turns and actings

    public void beginGame() {

    }

    public void markMissedTurn(Message message) {

    }

    public void markHitTurn(Message message) {

    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }
}
