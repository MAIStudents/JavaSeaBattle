package ru.mai.lessons.rpks;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Главный класс приложения сервера для игры "Морской бой".
 * Этот класс отвечает за инициализацию и запуск графического
 * интерфейса сервера.
 */
public final class Server extends Application {

  /**
   * Метод, который запускает приложение. Он загружает FXML-файл,
   * устанавливает контроллер и настраивает сцену.
   *
   * @param stage основной этап приложения
   * @throws IOException если возникает ошибка при загрузке FXML-файла
   */
  @Override
  public void start(Stage stage) throws IOException {
    FXMLLoader fxmlLoader
        = new FXMLLoader(Server.class.getResource("seaBattle.fxml"));

    SeaBattleServerController controller = new SeaBattleServerController();
    fxmlLoader.setController(controller);

    Scene scene = new Scene(fxmlLoader.load(), 992, 768);
    stage.setTitle("Java");
    stage.setScene(scene);

    // Обработчик закрытия окна
    stage.setOnCloseRequest(_ -> {
      Common.sendMessage("CLOSE", controller.getOut());
      ServerSocketController.getInstance().stop();
      controller.stopServer();
    });

    stage.show();
  }

  /**
   * Точка входа в приложение. Запускает серверное приложение.
   *
   * @param args аргументы командной строки
   */
  public static void main(final String[] args) {
    launch();
  }
}
