package ru.mai.lessons.rpks;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Контроллер для серверной части игры "Морской бой".
 * Этот класс управляет взаимодействием с клиентами и игровым процессом.
 */
public class SeaBattleServerController {

  /**
   * Адрес сервера, по которому будет запущен сервер.
   */
  private static final String SERVER_ADDRESS = "127.0.0.1";

  /**
   * Порт, на котором будет слушать сервер.
   */
  private static final int PORT = 12345;

  /**
   * Поток для обработки работы сервера.
   */
  private Thread serverThread;

  /** Размер одной клетки поля. */
  private static final double CELL_SIZE = 21.4;

  /** Светлый серый цвет для отображения состояния клеток. */
  private static final Color DARK_GRAY_COLOR
      = Color.color(0.13, 0.13, 0.13);

  /** Красный цвет для отображения состояния клеток. */
  private static final Color RED_COLOR
      = Color.color(1, 0.33, 0.33);

  /** Флаг соединения с пользователем. */
  private boolean connectionWithUser = false;

  /**
   * Серверный сокет, который принимает входящие соединения от клиентов.
   */
  private ServerSocket serverSocket;

  /**
   * Объект, представляющий игровую логику и состояние игры "Морской бой".
   */
  private SeaBattleGame seaBattleGame;

  /**
   * Поток для вывода сообщений клиенту.
   */
  private PrintWriter out;

  /**
   * Поток для ввода сообщений от клиента.
   */
  private BufferedReader in;

  @FXML
  private Button battleship; // Кнопка для линкора
  @FXML
  private Button cruisers; // Кнопка для крейсера
  @FXML
  private Button destroyer; // Кнопка для эсминца
  @FXML
  private Button torpedoBoat; // Кнопка для торпедного катера
  @FXML
  private Button readyButton; // Кнопка готовности
  @FXML
  private Button reconnect; // Кнопка повторного подключения
  @FXML
  private Button clearField; // Кнопка очистки поля
  @FXML
  private GridPane opponentField; // Поле противника
  @FXML
  private TextArea statusField; // Поле статуса
  @FXML
  private GridPane yourField; // Поле игрока

  /**
   * Инициализирует контроллер.
   * Запускает сервер и настраивает обработчики событий.
   */
  @FXML
  void initialize() {
    try {
      startServer();
    } catch (IOException e) {
      System.out.println(e.getMessage());
    }

    seaBattleGame = new SeaBattleGame(statusField);
    seaBattleGame.initializeContainers();
    setupFieldClickHandlers();
    setupBoatSelectionHandlers();
    setupReadyButtonHandler();
    setupReconnectButton();
    setupClearFieldButton();
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
      if (connectionWithUser) {
        if (seaBattleGame.toggleReady(getOut())) {
          disableBoatAndReadyButtons();
          if (seaBattleGame.isReady() && seaBattleGame.isOpponentReady()) {
            startGame();
          }
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
   * Настраивает кнопку повторного подключения.
   */
  private void setupReconnectButton() {
    reconnect.setDisable(true);
    reconnect.setVisible(false);
  }

  /**
   * Настраивает кнопку очистки игрового поля.
   */
  private void setupClearFieldButton() {
    clearField.setOnAction(_ -> seaBattleGame.clearPlayerField(yourField));
  }

  /**
   * Запускает сервер.
   * Создает серверный сокет и обрабатывает входящие подключения.
   *
   * @throws IOException если не удается создать серверный сокет
   */
  private void startServer() throws IOException {
    stopServer(); // Остановить сервер, если он уже запущен

    serverThread = new Thread(() -> {
      try {
        serverSocket = new ServerSocket(PORT);
        seaBattleGame.updateTextArea("Server launched. Wait for connection...");

        Runtime.getRuntime().addShutdownHook(new Thread(()
            -> Common.sendMessage("CLOSE", getOut())));

        ServerSocketController.getInstance().start(serverSocket);

        while (ServerSocketController.getInstance().isRunning()) {
          Socket clientSocket = serverSocket.accept();
          seaBattleGame.updateTextArea("Client connected: "
              + clientSocket.getInetAddress());
          connectionWithUser = true;
          handleClient(clientSocket);
        }
      } catch (IOException e) {
        System.out.println("Socket closed.");
      }
    });
    serverThread.start();
  }

  /**
   * Обрабатывает подключение клиента.
   *
   * @param clientSocket сокет клиента
   * @throws IOException если возникает ошибка ввода-вывода
   */
  private void handleClient(final Socket clientSocket)
      throws IOException {
    try {
      in = new BufferedReader(
          new InputStreamReader(clientSocket.getInputStream())
      );
      setOut(new PrintWriter(clientSocket.getOutputStream(), true));

      String inputLine;
      while ((inputLine = in.readLine()) != null) {
        processClientMessage(inputLine);
      }
    } catch (IOException e) {
      handleClientDisconnection();
    }
  }

  /**
   * Обрабатывает сообщения от клиента.
   *
   * @param inputLine строка сообщения от клиента
   */
  private void processClientMessage(final String inputLine) {
    switch (inputLine) {
      case "READY":
        handleOpponentReady();
        break;
      case "LOSE":
        handleOpponentLoss();
        break;
      case "CLOSE":
        handleGameClose();
        break;
      default:
        if (inputLine.contains("YES") || inputLine.contains("NO")) {
          handleHitOrMiss(inputLine);
        } else if (inputLine.contains("COORDINATES")) {
          handleCoordinates(inputLine);
        } else {
          seaBattleGame.updateTextArea(inputLine);
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
      startGame();
    } else {
      seaBattleGame.updateTextArea("Opponent is ready");
    }
  }

  /**
   * Обрабатывает проигрыш противника.
   */
  private void handleOpponentLoss() {
    seaBattleGame.setEnd(true);
    seaBattleGame.updateTextArea("You're lose. :(");
  }

  /**
   * Обрабатывает завершение игры.
   */
  private void handleGameClose() {
    seaBattleGame.clearGameData(yourField, opponentField);
    resetGameControls();
    connectionWithUser = false;
    seaBattleGame.updateTextArea("The game is interrupted.");
    seaBattleGame.updateTextArea("The opponent has switched off.");
  }

  /**
   * Сбрасывает элементы управления игры.
   */
  private void resetGameControls() {
    seaBattleGame.setActive(torpedoBoat);
    seaBattleGame.setActive(destroyer);
    seaBattleGame.setActive(cruisers);
    seaBattleGame.setActive(battleship);
    seaBattleGame.setActive(readyButton);
    seaBattleGame.setActive(clearField);
  }

  /**
   * Обрабатывает попадание или промах по кораблю.
   *
   * @param inputLine строка с результатом атаки ("YES" или "NO")
   */
  private void handleHitOrMiss(final String inputLine) {
    String finalResponse = inputLine;
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
   * @param inputLine строка с координатами
   */
  private void handleCoordinates(final String inputLine) {
    String[] stringArray = inputLine.split(", ");
    int i = Integer.parseInt(stringArray[1]);
    int j = Integer.parseInt(stringArray[2]);

    seaBattleGame.updateTextArea("Opponent move "
        + SeaBattleGame.SYMBOLS[i - 1] + j);
    boolean hit = seaBattleGame.attackCell(i, j, yourField);
    Common.sendMessage(hit
        ? "YES, " + i + ", " + j
        : "NO, " + i + ", " + j, getOut());
    seaBattleGame.setYourMove(!hit);
  }

  /**
   * Останавливает сервер.
   */
  protected void stopServer() {
    if (serverThread != null && !serverThread.isInterrupted()) {
      Common.sendMessage("CLOSE", getOut());
      serverThread.interrupt();
    }
  }

  /**
   * Запускает игру.
   * Устанавливает начальные условия для игры и определяет, кто ходит первым.
   */
  private void startGame() {
    seaBattleGame.updateTextArea("Game is started");
    seaBattleGame.setStart(true);
    seaBattleGame.setYourMove(Math.round(Math.random()) == 1);
    seaBattleGame.updateTextArea(seaBattleGame.isYourMove()
        ? "You move first"
        : "Opponent move first");
    Common.sendMessage(seaBattleGame.isYourMove()
        ? "Opponent move first"
        : "MOVE", out);
  }

  /**
   * Обрабатывает отключение клиента.
   */
  private void handleClientDisconnection() throws IOException {
    seaBattleGame.clearGameData(yourField, opponentField);
    resetGameControls();
    seaBattleGame.updateTextArea("The server crashed. Try to reload...");
    startServer();
  }

  /**
   * Получает поток для вывода сообщений.
   *
   * @return поток для вывода сообщений
   */
  public PrintWriter getOut() {
    return out;
  }

  /**
   * Устанавливает поток для вывода сообщений.
   *
   * @param out поток для вывода сообщений
   */
  private void setOut(final PrintWriter out) {
    this.out = out;
  }
}