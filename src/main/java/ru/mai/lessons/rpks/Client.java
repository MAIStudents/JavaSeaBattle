package ru.mai.lessons.rpks;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Главный класс клиентского приложения для игры "Морской бой".
 * Этот класс отвечает за инициализацию и запуск графического интерфейса клиента.
 */
public final class Client extends Application {

    /**
     * Метод, который запускает клиентское приложение. Он загружает FXML-файл,
     * устанавливает контроллер и настраивает сцену.
     *
     * @param stage основной этап приложения
     * @throws IOException если возникает ошибка при загрузке FXML-файла
     */
    @Override
    public void start(final Stage stage) throws IOException {
        FXMLLoader fxmlLoader
            = new FXMLLoader(Client.class.getResource("seaBattle.fxml"));

        SeaBattleUserController controller = new SeaBattleUserController();
        fxmlLoader.setController(controller);

        Scene scene = new Scene(fxmlLoader.load(), 992, 768);
        stage.setTitle("Java");
        stage.setScene(scene);

        // Обработчик закрытия окна
        stage.setOnCloseRequest(_ -> {
            Common.sendMessage("CLOSE", controller.getOut());
            UserSocketController.getInstance().stop();
        });

        stage.show();
    }

    /**
     * Точка входа в клиентское приложение.
     * Запускает клиентское приложение.
     *
     * @param args аргументы командной строки
     */
    public static void main(final String[] args) {
        launch();
    }
}
