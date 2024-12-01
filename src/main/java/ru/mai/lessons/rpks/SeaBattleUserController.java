package ru.mai.lessons.rpks;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Контроллер для пользовательского интерфейса игры "Морской бой".
 * Этот класс управляет взаимодействием пользователя с игрой и сервером.
 */
public class SeaBattleUserController {

  /** Адрес сервера. */
  private static final String SERVER_ADDRESS = "127.0.0.1";

  /** Порт для подключения к серверу. */
  private static final int PORT = 12345;

  /** Поток для обработки пользовательских операций. */
  private Thread userThread;

  /** Размер одной клетки поля. */
  private static final double CELL_SIZE = 21.4;

  /** Темный серый цвет для отображения состояния клеток. */
  private static final Color DARK_GRAY_COLOR
      = Color.color(0.13, 0.13, 0.13);

  /** Красный цвет для отображения состояния клеток. */
  private static final Color RED_COLOR
      = Color.color(1, 0.33, 0.33);

  @FXML
  private Button battleship; // Кнопка для выбора линкора
  @FXML
  private Button cruisers; // Кнопка для выбора крейсера
  @FXML
  private Button destroyer; // Кнопка для выбора эсминца
  @FXML
  private Button torpedoBoat; // Кнопка для выбора торпедного катера
  @FXML
  private Button readyButton; // Кнопка для подтверждения готовности
  @FXML
  private Button reconnect; // Кнопка для повторного подключения к серверу
  @FXML
  private Button clearField; // Кнопка для очистки игрового поля
  @FXML
  private GridPane opponentField; // Поле противника
  @FXML
  private TextArea statusField; // Поле для отображения статуса игры
  @FXML
  private GridPane yourField; // Поле игрока

  /** Поток для вывода сообщений. */
  private PrintWriter out;

  /** Поток для ввода сообщений. */
  private BufferedReader in;

  /** Флаг соединения с сервером. */
  private boolean connectionWithServer = false;

  /** Объект игры "Морской бой". */
  private SeaBattleGame seaBattleGame;

  /**
   * Получает поток для вывода сообщений.
   *
   * @return поток для вывода сообщений
   */
  public PrintWriter getOut() {
    return out;
  }

  /**
   * Инициализирует контроллер.
   * Настраивает взаимодействие с игровыми полями и кнопками.
   */
  @FXML
  void initialize() {
    seaBattleGame = new SeaBattleGame(statusField);
    seaBattleGame.initializeContainers();
    setupFieldClickHandlers();
    setupBoatSelectionHandlers();
    setupReadyButtonHandler();
    setupReconnectButtonHandler();
    setupClearFieldButtonHandler();
    connectToServer();
  }

  /**
   * Настраивает обработчики кликов на игровых полях.
   */
  private void setupFieldClickHandlers() {
    yourField.setOnMouseClicked(mouseEvent ->
        seaBattleGame.handleMouseClick(mouseEvent,
            yourField, true, getOut()));
    opponentField.setOnMouseClicked(mouseEvent ->
        seaBattleGame.handleMouseClick(mouseEvent,
            opponentField, false, getOut()));
  }

  /**
   * Настраивает обработчики выбора кораблей.
   */
  private void setupBoatSelectionHandlers() {
    torpedoBoat.setOnAction(_
        -> seaBattleGame.changeSelectedBoat(SeaBattleGame.Boat.TorpedoBoat));
    battleship.setOnAction(_
        -> seaBattleGame.changeSelectedBoat(SeaBattleGame.Boat.Battleship));
    destroyer.setOnAction(_
        -> seaBattleGame.changeSelectedBoat(SeaBattleGame.Boat.Destroyer));
    cruisers.setOnAction(_
        -> seaBattleGame.changeSelectedBoat(SeaBattleGame.Boat.Cruisers));
  }

  /**
   * Настраивает обработчик кнопки готовности.
   * Подтверждает готовность игрока и отключает соответствующие кнопки.
   */
  private void setupReadyButtonHandler() {
    readyButton.setOnAction(_ -> {
      if (connectionWithServer) {
        if (seaBattleGame.toggleReady(getOut())) {
          disableBoatAndReadyButtons();
        }
      } else {
        seaBattleGame.updateTextArea("The opponent is not connected");
      }
    });
  }

  /**
   * Отключает кнопки выбора кораблей и кнопку готовности.
   */
  private void disableBoatAndReadyButtons() {
    seaBattleGame.setDisable(torpedoBoat);
    seaBattleGame.setDisable(destroyer);
    seaBattleGame.setDisable(cruisers);
    seaBattleGame.setDisable(battleship);
    seaBattleGame.setDisable(readyButton);
    seaBattleGame.setDisable(clearField);
  }

  /**
   * Настраивает обработчик кнопки повторного подключения к серверу.
   */
  private void setupReconnectButtonHandler() {
    reconnect.setOnAction(_ -> {
      if (!connectionWithServer) {
        connectToServer();
      }
    });
  }

  /**
   * Настраивает обработчик кнопки очистки игрового поля.
   */
  private void setupClearFieldButtonHandler() {
    clearField.setOnAction(_ -> seaBattleGame.clearPlayerField(yourField));
  }

  /**
   * Устанавливает соединение с сервером.
   * Создает сокет и запускает поток для обработки входящих сообщений.
   */
  private void connectToServer() {
    try {
      Socket socket = new Socket(SERVER_ADDRESS, PORT);
      UserSocketController.getInstance().start(socket);

      out = new PrintWriter(socket.getOutputStream(), true);
      in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

      startUserThread();
    } catch (IOException e) {
      seaBattleGame.updateTextArea("Server is don't init.");
    }
  }

  /**
   * Запускает поток для обработки пользовательских операций.
   */
  private void startUserThread() {
    userThread = new Thread(() -> {
      String response;
      seaBattleGame.updateTextArea("Connection with server is successfully.");
      connectionWithServer = true;
      seaBattleGame.setDisable(reconnect);

      try {
        while ((response = in.readLine()) != null) {
          handleServerResponse(response);
        }
      } catch (IOException e) {
        seaBattleGame.setActive(reconnect);
      }
    });
    userThread.start();
  }

  /**
   * Обрабатывает ответ от сервера.
   *
   * @param response строка с ответом от сервера
   */
  private void handleServerResponse(final String response) {
    switch (response) {
      case "READY":
        handleOpponentReady();
        break;
      case "CLOSE":
        handleGameClose();
        break;
      case "LOSE":
        handleGameLoss();
        break;
      case "MOVE":
        handleYourMove();
        break;
      default:
        if (response.contains("YES") || response.contains("NO")) {
          handleHitOrMiss(response);
        } else if (response.contains("COORDINATES")) {
          handleCoordinates(response);
        } else {
          seaBattleGame.updateTextArea(response);
        }
        break;
    }
  }

  /**
   * Обрабатывает готовность противника.
   */
  private void handleOpponentReady() {
    seaBattleGame.setOpponentReady(true);
    if (seaBattleGame.isReady()) {
      seaBattleGame.updateTextArea("Game is started.");
      seaBattleGame.setStart(true);
    } else {
      seaBattleGame.updateTextArea("Opponent is ready.");
    }
  }

  /**
   * Обрабатывает завершение игры.
   */
  private void handleGameClose() {
    seaBattleGame.clearGameData(yourField, opponentField);
    seaBattleGame.updateTextArea("The game is interrupted.");
    connectionWithServer = false;
    resetGameControls();
    UserSocketController.getInstance().stop();
    userThread.interrupt();
  }

  /**
   * Сбрасывает элементы управления игры в исходное состояние.
   */
  private void resetGameControls() {
    seaBattleGame.setActive(torpedoBoat);
    seaBattleGame.setActive(destroyer);
    seaBattleGame.setActive(cruisers);
    seaBattleGame.setActive(battleship);
    seaBattleGame.setActive(readyButton);
    seaBattleGame.setActive(clearField);
    seaBattleGame.setActive(reconnect);
  }

  /**
   * Обрабатывает проигрыш игрока.
   */
  private void handleGameLoss() {
    seaBattleGame.setEnd(true);
    seaBattleGame.updateTextArea("You're lose. :(");
  }

  /**
   * Обрабатывает ход игрока.
   */
  private void handleYourMove() {
    seaBattleGame.setYourMove(true);
    seaBattleGame.updateTextArea("Your move first");
  }

  /**
   * Обрабатывает попадание или промах по кораблю.
   *
   * @param response строка с результатом атаки ("YES" или "NO")
   */
  private void handleHitOrMiss(final String response) {
    String finalResponse = response;
    Platform.runLater(() -> {
      String[] stringArray = finalResponse.split(", ");
      Rectangle rectangle = new Rectangle(CELL_SIZE, CELL_SIZE,
          stringArray[0].equals("YES")
              ? RED_COLOR
              : DARK_GRAY_COLOR);
      opponentField.add(rectangle, Integer.parseInt(stringArray[1]),
          Integer.parseInt(stringArray[2]));
      seaBattleGame.setYourMove(stringArray[0].equals("YES"));
      if (seaBattleGame.isYourMove()) {
        seaBattleGame.setScore(seaBattleGame.getScore() + 1);
      }

      if (seaBattleGame.getScore() == 20) {
        seaBattleGame.setEnd(true);
        Common.sendMessage("LOSE", getOut());
        seaBattleGame.updateTextArea("You're win!!!");
      }
    });
  }

  /**
   * Обрабатывает координаты, полученные от противника.
   *
   * @param response строка с координатами
   */
  private void handleCoordinates(String response) {
    String[] stringArray = response.split(", ");
    int i = Integer.parseInt(stringArray[1]);
    int j = Integer.parseInt(stringArray[2]);

    seaBattleGame.updateTextArea("Opponent move "
        + SeaBattleGame.SYMBOLS[i - 1] + j + ".");
    boolean hit = seaBattleGame.attackCell(i, j, yourField);
    Common.sendMessage(hit
        ? "YES, " + i + ", " + j
        : "NO, " + i + ", " + j, getOut());
    seaBattleGame.setYourMove(!hit);
  }
}