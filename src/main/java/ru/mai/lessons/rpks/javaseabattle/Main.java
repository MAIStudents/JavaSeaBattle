package ru.mai.lessons.rpks.javaseabattle;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import ru.mai.lessons.rpks.javaseabattle.controller.Controller;
import ru.mai.lessons.rpks.javaseabattle.controller.game_control_button_state.GameControlButtonState;

import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

public class Main extends Application {

    private static final Locale LOCALE_RU = Locale.forLanguageTag("ru_RU");

    public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("Messages_ru_RU", LOCALE_RU);

    private Controller controller;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("sea-battle-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());

        controller = fxmlLoader.getController();

        stage.setResizable(false);
        stage.setTitle(BUNDLE.getString("root.appTitle"));
        stage.setScene(scene);

        stage.setOnCloseRequest(event -> {
            try {
                if (controller.gameControlButtonState.equals(GameControlButtonState.PREVENT_CLOSING)) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle(Main.BUNDLE.getString("root.closingPreventTitle"));
                    alert.setHeaderText(Main.BUNDLE.getString("root.closingPreventText"));
                    alert.show();
                    event.consume();
                } else if (!controller.gameControlButtonState.equals(GameControlButtonState.SET_SHIPS)) {
                    controller.gameControlButton();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }

}