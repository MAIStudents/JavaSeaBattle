package ru.mai.lessons.rpks.Server.include;

import ru.mai.lessons.rpks.controllers.MessageController;
import ru.mai.lessons.rpks.include.GameEvent;


public class GameLogic {
  private final ConnectionHandler player1;
  private final ConnectionHandler player2;
  private static final long MAX_BEAT_WAIT = 5000L;
  private long lastBeatPlayer1 = System.currentTimeMillis();
  private long lastBeatPlayer2 = System.currentTimeMillis();

  public GameLogic(ConnectionHandler player1, ConnectionHandler player2) {
    this.player1 = player1;
    this.player2 = player2;
  }

  public void startGameLoop() {
    try {
      initializeGame();

      int currentPlayer = 0;
      GameState state = GameState.NOPE;

      while (state != GameState.GAME_OVER || !player1.isClosed() || !player2.isClosed()) {
        MessageController message1 = getHeartbeatOrMessage(player1, 1);
        MessageController message2 = getHeartbeatOrMessage(player2, 2);

        checkConnectionTimeout();

        state = processGameState(currentPlayer, state, message1, message2);

        if (state == GameState.CHANGE_MOVE) {
          currentPlayer = (currentPlayer + 1) % 2;
          state = GameState.STAY_MOVE;
        }

        if (state == GameState.STAY_MOVE) {
          sendReadyMessage(currentPlayer);
          state = GameState.NOPE;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      closeConnections();
    }
  }

  private void initializeGame() {
    player1.sendMessage(String.valueOf(GameState.HEARTBEAT.ordinal()));
    player2.sendMessage(String.valueOf(GameState.HEARTBEAT.ordinal()));
    player1.sendMessage(String.valueOf(GameState.STAY_MOVE.ordinal()));

  }

  private MessageController getHeartbeatOrMessage(ConnectionHandler player, int playerNumber) {
    MessageController message = player.getNextMessage();
    if (message != null && message.getMessageType() == MessageController.MessageType.HEARTBEAT) {
      if (playerNumber == 1) {
        lastBeatPlayer1 = System.currentTimeMillis();
      } else {
        lastBeatPlayer2 = System.currentTimeMillis();
      }
      return null;
    }
    return message;
  }

  private void checkConnectionTimeout() {
    long currentTime = System.currentTimeMillis();
    if (currentTime - lastBeatPlayer1 > MAX_BEAT_WAIT || currentTime - lastBeatPlayer2 > MAX_BEAT_WAIT) {
      sendMessageToPlayers();
      throw new RuntimeException("A player disconnected due to timeout.");
    }
  }

  private GameState processGameState(int currentPlayer, GameState state, MessageController message1, MessageController message2) {
    if (currentPlayer == 0 && state == GameState.NOPE && message1 != null) {
      return makeMove(message1, player1, player2);
    } else if (currentPlayer == 1 && state == GameState.NOPE && message2 != null) {
      return makeMove(message2, player2, player1);
    } else if (currentPlayer == 0 && state == GameState.WAITING && message2 != null) {
      return makeMove(message2, player1, player2);
    } else if (currentPlayer == 1 && state == GameState.WAITING && message1 != null) {
      return makeMove(message1, player2, player1);
    }
    return state;
  }

  private void sendReadyMessage(int currentPlayer) {
    if (currentPlayer == 0) {
      player1.sendMessage(String.valueOf(GameState.STAY_MOVE.ordinal()));
    } else {
      player2.sendMessage(String.valueOf(GameState.STAY_MOVE.ordinal()));
    }
  }

  private void sendMessageToPlayers() {
    player1.sendMessage(String.valueOf(GameState.GAME_OVER.ordinal()));
    player2.sendMessage(String.valueOf(GameState.GAME_OVER.ordinal()));
  }

  private void closeConnections() {
    player1.closeConnections();
    player2.closeConnections();
  }

  private GameState makeMove(MessageController message, ConnectionHandler sender, ConnectionHandler receiver) {
    switch (message.getMessageType()) {
      case STEP:
        receiver.sendMessage(message.toString());
        return GameState.WAITING;
      case GAME_OVER:
        sender.sendMessage(message.toString());
        return GameState.GAME_OVER;
      case RESPONSE:
        sender.sendMessage(message.toString());
        return GameEvent.containsOnlyMissed(message.getGameEvents()) ? GameState.CHANGE_MOVE : GameState.STAY_MOVE;
      case HEARTBEAT:
        return GameState.HEARTBEAT;
      default:
        return GameState.INVALID;
    }
  }
  private enum GameState {
    WAITING,
    CHANGE_MOVE,
    NOPE,
    STAY_MOVE,
    GAME_OVER,
    INVALID,
    HEARTBEAT,
  }
}
