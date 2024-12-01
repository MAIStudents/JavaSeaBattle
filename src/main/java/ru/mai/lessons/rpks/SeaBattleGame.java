package ru.mai.lessons.rpks;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.io.PrintWriter;
import java.util.*;

/**
 * Класс, представляющий игру "Морской бой".
 */
public final class SeaBattleGame {

  /** Светлый цвет для отображения состояния клеток. */
  private static final Color LIGHT_COLOR
      = Color.color(0.8, 0.8, 0.8);

  /** Темный серый цвет для отображения состояния клеток. */
  private static final Color DARK_GRAY_COLOR
      = Color.color(0.13, 0.13, 0.13);

  /** Красный цвет для отображения состояния клеток. */
  private static final Color RED_COLOR
      = Color.color(1, 0.33, 0.33);

  /** Размер поля. */
  private static final int FIELD_SIZE = 11;

  /** Размер одной клетки поля. */
  private static final double CELL_SIZE = 21.4;

  /** Символы для обозначения столбцов. */
  public static final char[] SYMBOLS
      = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J'};

  /**
   * Перечисление, представляющее типы кораблей.
   */
  enum Boat {
    Battleship,
    Cruisers,
    Destroyer,
    TorpedoBoat
  }

  /** Геттер для поля score */
  public int getScore() {
    return score;
  }

  /** Сеттер для поля score */
  public void setScore(final int score) {
    this.score = score;
  }

  /** Геттер для поля ready */
  public boolean isReady() {
    return ready;
  }

  /** Сеттер для поля ready */
  public void setReady(final boolean ready) {
    this.ready = ready;
  }

  /** Геттер для поля opponentIsReady */
  public boolean isOpponentReady() {
    return opponentIsReady;
  }

  /** Сеттер для поля opponentIsReady */
  public void setOpponentReady(final boolean opponentIsReady) {
    this.opponentIsReady = opponentIsReady;
  }

  /** Сеттер для поля start */
  public void setStart(final boolean start) {
    this.start = start;
  }

  /** Геттер для поля end */
  public boolean isEnd() {
    return end;
  }

  /** Сеттер для поля end */
  public void setEnd(final boolean end) {
    this.end = end;
  }

  /** Геттер для поля yourMove */
  public boolean isYourMove() {
    return yourMove;
  }

  /** Сеттер для поля yourMove */
  public void setYourMove(final boolean yourMove) {
    this.yourMove = yourMove;
  }

  /** Поле для отображения статуса игры. */
  private final TextArea statusField;

  /** Список статусов клеток игрока. */
  private final List<List<Integer>> cellsStatus
      = new ArrayList<>(FIELD_SIZE);

  /** Список статусов клеток противника. */
  private final List<List<Boolean>> cellsStatusOpponent
      = new ArrayList<>(FIELD_SIZE);

  /** Выбранный корабль. */
  private Boat selectedBoat = null;

  /** Количество кораблей каждого типа. */
  private final Map<Boat, Integer> shipCount = new HashMap<>();

  /** Текущий счет игрока. */
  private int score = 0;

  /** Проверяет, готов ли игрок. */
  private boolean ready = false;

  /** Проверяет, готов ли противник. */
  private boolean opponentIsReady = false;

  /** Старт игры. */
  private boolean start = false;

  /** Конец игры. */
  private boolean end = false;

  /** Проверяет, чей ход. */
  private boolean yourMove = false;

  /**
   * Конструктор класса SeaBattleGame.
   *
   * @param statusField поле для отображения статуса игры
   */
  public SeaBattleGame(final TextArea statusField) {
    this.statusField = statusField;
  }

  /**
   * Инициализирует контейнеры для статусов клеток.
   */
  public void initializeContainers() {
    for (int i = 0; i < FIELD_SIZE; i++) {
      cellsStatus.add(new ArrayList<>(Collections.nCopies(FIELD_SIZE, 0)));
      cellsStatusOpponent.add(
          new ArrayList<>(Collections.nCopies(FIELD_SIZE, false))
      );
    }

    shipCount.put(Boat.Battleship,  1);
    shipCount.put(Boat.Cruisers,    2);
    shipCount.put(Boat.Destroyer,   3);
    shipCount.put(Boat.TorpedoBoat, 4);
  }

  /**
   * Переключает состояние готовности игрока.
   *
   * @param out поток для вывода сообщений
   * @return true, если игрок готов, иначе false
   */
  boolean toggleReady(PrintWriter out) {
    for (int count: shipCount.values()) {
      if (count != 0) {
        updateTextArea("You don't set all ships");
        return false;
      }
    }

    ready = true;
    Common.sendMessage("READY", out);

    if (!opponentIsReady) {
      updateTextArea("Wait opponent.");
    } else {
      start = true;
    }

    return true;
  }

  /**
   * Меняет выбранный корабль.
   *
   * @param boat выбранный корабль
   */
  public void changeSelectedBoat(Boat boat) {
    if (shipCount.get(boat) != 0) {
      selectedBoat = boat;
    }
  }

  /**
   * Обрабатывает клик мыши по полю.
   *
   * @param event событие клика мыши
   * @param gridPane поле игры
   * @param isYourField является ли поле игрока
   * @param out поток для вывода сообщений
   */
  public void handleMouseClick(final MouseEvent event,
                               final GridPane gridPane,
                               final boolean isYourField,
                               final PrintWriter out) {
    if ((!start || !isYourField) && !end) {
      double x = event.getX(), y = event.getY();

      int columnIndex = (int) (x / (gridPane.getWidth() / FIELD_SIZE));
      int rowIndex = (int) (y / (gridPane.getHeight() / FIELD_SIZE));

      if (columnIndex != 0 && rowIndex != 0) {
        if (selectedBoat != null && isYourField && !start) {
          switch (selectedBoat) {
            case Battleship
             -> reserveCells(Boat.Battleship, columnIndex, rowIndex, gridPane);
            case Cruisers
             -> reserveCells(Boat.Cruisers, columnIndex, rowIndex, gridPane);
            case Destroyer
             -> reserveCells(Boat.Destroyer, columnIndex, rowIndex, gridPane);
            case TorpedoBoat
             -> reserveCells(Boat.TorpedoBoat, columnIndex, rowIndex, gridPane);
          }
        } else if (start && yourMove) {
          if (!cellsStatusOpponent.get(columnIndex).get(rowIndex)) {
            cellsStatusOpponent.get(columnIndex).set(rowIndex, true);
            Common.sendMessage("COORDINATES, "
                + columnIndex + ", " + rowIndex, out);
          } else {
            updateTextArea("Select another cell.");
          }
        }
      } else {
        updateTextArea("Select another cell.");
      }
    }
  }

  /**
   * Резервирует клетки для выбранного корабля.
   *
   * @param boat тип корабля
   * @param i индекс строки
   * @param j индекс столбца
   * @param yourField поле игрока
   */
  public void reserveCells(final Boat boat,
                           final int i,
                           final int j,
                           final GridPane yourField) {
    if (shipCount.get(boat) <= 0) {
      return;
    }

    int length = getBoatLength(boat);
    boolean horizontal = true;

    if (j + length <= FIELD_SIZE) {
      if (canPlaceBoat(i, j, length, horizontal)) {
        placeBoat(i, j, length, horizontal);

        shipCount.put(boat, shipCount.get(boat) - 1);
        updateGridPane(yourField);

        return;
      }
    }

    if (i + length <= FIELD_SIZE) {
      horizontal = false;
      if (canPlaceBoat(i, j, length, horizontal)) {
        placeBoat(i, j, length, horizontal);

        shipCount.put(boat, shipCount.get(boat) - 1);
        updateGridPane(yourField);

        return;
      }
    }
    updateTextArea("Cannot place the boat at the specified location.");
  }

  /**
   * Получает длину указанного корабля.
   *
   * @param boat тип корабля
   * @return длина корабля
   */
  private int getBoatLength(final Boat boat) {
    return switch (boat) {
      case Battleship -> 4;
      case Cruisers -> 3;
      case Destroyer -> 2;
      case TorpedoBoat -> 1;
    };
  }

  /**
   * Проверяет, можно ли разместить корабль на указанной позиции.
   *
   * @param i индекс строки
   * @param j индекс столбца
   * @param length длина корабля
   * @param horizontal горизонтальное размещение
   * @return true, если корабль можно разместить, иначе false
   */
  private boolean canPlaceBoat(final int i,
                               final int j,
                               final int length,
                               final boolean horizontal) {
    for (int k = 0; k < length; k++) {
      int x = horizontal ? i : i + k;
      int y = horizontal ? j + k : j;
      if (cellsStatus.get(x).get(y) != 0) {
        return false;
      }
    }
    return true;
  }

  /**
   * Размещает корабль на указанной позиции.
   *
   * @param i индекс строки
   * @param j индекс столбца
   * @param length длина корабля
   * @param horizontal горизонтальное размещение
   */
  private void placeBoat(final int i,
                         final int j,
                         final int length,
                         final boolean horizontal) {
    for (int k = 0; k < length; k++) {
      int x = horizontal ? i : i + k;
      int y = horizontal ? j + k : j;
      cellsStatus.get(x).set(y, 2);
      markSurroundingCells(x, y);
    }
  }

  /**
   * Помечает окружающие клетки вокруг размещенного корабля.
   *
   * @param x индекс строки
   * @param y индекс столбца
   */
  private void markSurroundingCells(final int x,
                                    final int y) {
    for (int dx = -1; dx <= 1; dx++) {
      for (int dy = -1; dy <= 1; dy++) {
        if (dx != 0 || dy != 0) {
          int newX = x + dx;
          int newY = y + dy;
          if (isInBounds(newX, newY) && cellsStatus.get(newX).get(newY) == 0) {
            cellsStatus.get(newX).set(newY, 1);
          }
        }
      }
    }
  }

  /**
   * Проверяет, находятся ли указанные индексы в пределах поля.
   *
   * @param x индекс строки
   * @param y индекс столбца
   * @return true, если индексы в пределах, иначе false
   */
  private boolean isInBounds(final int x,
                             final int y) {
    return x >= 0 && x < FIELD_SIZE && y >= 0 && y < FIELD_SIZE;
  }

  /**
   * Атакует клетку на указанной позиции.
   *
   * @param column индекс столбца
   * @param row индекс строки
   * @param yourField поле игрока
   * @return true, если атака успешна, иначе false
   */
  public boolean attackCell(final int column,
                            final int row,
                            final GridPane yourField) {
    if (cellsStatus.get(column).get(row) == 2) {
      updateGridPane(true, column, row, yourField);
      return true;
    }

    updateGridPane(false, column, row, yourField);
    return false;
  }

  /**
   * Обновляет поле игрока.
   *
   * @param yourField поле игрока
   */
  private void updateGridPane(final GridPane yourField) {
    for(int i = 1; i < FIELD_SIZE; i++) {
      for(int j = 1; j < FIELD_SIZE; j++) {
        if (cellsStatus.get(i).get(j) == 2) {
          Rectangle rectangle
              = new Rectangle(CELL_SIZE, CELL_SIZE, LIGHT_COLOR);
          rectangle.setStroke(LIGHT_COLOR);
          yourField.add(rectangle, i, j);
        }
      }
    }
  }

  /**
   * Обновляет поле игрока в зависимости от результата атаки.
   *
   * @param toUpdate состояние обновления
   * @param i индекс строки
   * @param j индекс столбца
   * @param yourField поле игрока
   */
  private void updateGridPane(final boolean toUpdate,
                              final int i,
                              final int j,
                              final GridPane yourField) {
    Platform.runLater(() -> {
      Rectangle rectangle;
      if (toUpdate) {
        Node existingNode = getNodeFromGridPane(i, j, yourField);
        assert existingNode != null;
        yourField.getChildren().remove(existingNode);
        rectangle = new Rectangle(CELL_SIZE, CELL_SIZE, RED_COLOR);
      } else {
        rectangle = new Rectangle(CELL_SIZE, CELL_SIZE, DARK_GRAY_COLOR);
      }

      yourField.add(rectangle, i, j);
    });
  }

  /**
   * Очищает данные игры.
   *
   * @param yourField поле игрока
   * @param opponentField поле противника
   */
  public void clearGameData(final GridPane yourField,
                            final GridPane opponentField) {
    ready = false;
    opponentIsReady = false;
    start = false;
    end = false;
    statusField.clear();

    score = 0;

    clearPlayerField(yourField);
    clearGridPane(opponentField);
  }

  /**
   * Очищает поле игрока.
   *
   * @param field поле игрока
   */
  public void clearPlayerField(final GridPane field) {
    clearGridPane(field);

    cellsStatus.clear();
    cellsStatusOpponent.clear();
    initializeContainers();
  }

  /**
   * Отключает кнопку.
   *
   * @param button кнопка для отключения
   */
  void setDisable(final Button button) {
    button.setStyle("-fx-opacity: 0.5; -fx-cursor: default;");
    button.setDisable(true);
  }

  /**
   * Активирует кнопку.
   *
   * @param button кнопка для активации
   */
  public void setActive(final Button button) {
    button.setStyle("-fx-opacity: 1; -fx-cursor: hand;");
    button.setDisable(false);
  }

  /**
   * Обновляет текстовое поле с сообщением.
   *
   * @param message сообщение для отображения
   */
  public void updateTextArea(final String message) {
    Platform.runLater(()
        -> this.statusField.appendText(message + "\n"));
  }

  /**
   * Получает узел из GridPane по указанным индексам.
   *
   * @param i индекс строки
   * @param j индекс столбца
   * @param gridPane поле игры
   * @return узел, если найден, иначе null
   */
  private Node getNodeFromGridPane(final int i,
                                   final int j,
                                   final GridPane gridPane) {
    for (Node node : gridPane.getChildren()) {
      var columnIndex = GridPane.getColumnIndex(node);
      var rowIndex = GridPane.getRowIndex(node);

      if (columnIndex != null && rowIndex != null
          && columnIndex == i && rowIndex == j) {
        return node;
      }
    }
    return null;
  }

  /**
   * Очищает GridPane.
   *
   * @param gridPane поле игры
   */
  private void clearGridPane(final GridPane gridPane) {
    Platform.runLater(() -> {
      for (int i = gridPane.getChildren().size() - 1; i > 0; i--) {
        Node node = gridPane.getChildren().get(i);
        var columnIndex = GridPane.getColumnIndex(node);
        var rowIndex = GridPane.getRowIndex(node);

        if (columnIndex != null
            && rowIndex != null
            && rowIndex >= 1 && columnIndex >= 1) {
          gridPane.getChildren().remove(node);
        }
      }
    });
  }
}