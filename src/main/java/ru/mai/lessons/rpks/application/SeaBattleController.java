package ru.mai.lessons.rpks.application;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import ru.mai.lessons.rpks.client.Client;
import ru.mai.lessons.rpks.server.Server;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SeaBattleController implements Initializable {
    @FXML
    private TextField textFieldHost;
    @FXML
    private TextField textFieldPort;
    private final ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    Server server;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        textFieldHost.setText("localhost");
        textFieldPort.setText("8843");
        server = new Server("localhost", 8843);
        server.start();
    }

    public void connect() {
        service.execute(() -> new Client(textFieldHost.getText(), Integer.parseInt(textFieldPort.getText())).start());
    }

    public void onClose() {
        server.stop();

        service.shutdown();

        try {
            if (!service.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
                service.shutdownNow();
            }
        } catch (InterruptedException e) {
            service.shutdownNow();
        }
    }
}
