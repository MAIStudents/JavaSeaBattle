package ru.mai.lessons.rpks.Server.include;

import ru.mai.lessons.rpks.controllers.MessageController;
import ru.mai.lessons.rpks.include.GameEvent;

public class GameManager {
  private final ConnectionHandler player1;
  private final ConnectionHandler player2;
  private static final long MAX_HEARTBEAT_TIMEOUT = 5000L;
  private long lastBeatPlayer1 = System.currentTimeMillis();
  private long lastBeatPlayer2 = System.currentTimeMillis();

  public GameManager(ConnectionHandler player1, ConnectionHandler player2) {
    this.player1 = player1;
    this.player2 = player2;
  }

  private void setupGame() {
    player1.sendMessage(String.valueOf(TurnState.CONNECTION_CHECK.ordinal()));
    player2.sendMessage(String.valueOf(TurnState.CONNECTION_CHECK.ordinal()));
    player1.sendMessage(String.valueOf(TurnState.KEEP_TURN.ordinal()));

  }

  private MessageController retrieveHeartbeatOrMessage(ConnectionHandler player, int playerNumber) {
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

  private void validateConnectionTimeout() {
    long currentTime = System.currentTimeMillis();
    if (currentTime - lastBeatPlayer1 > MAX_HEARTBEAT_TIMEOUT || currentTime - lastBeatPlayer2 > MAX_HEARTBEAT_TIMEOUT) {
      sendMessageToPlayers();
      throw new RuntimeException("A player disconnected due to timeout.");
    }
  }

  private TurnState handleGameState(int currentPlayer, TurnState state, MessageController message1, MessageController message2) {
    if (currentPlayer == 0 && state == TurnState.INVALID_MOVE && message1 != null) {
      return makeMove(message1, player1, player2);
    } else if (currentPlayer == 1 && state == TurnState.INVALID_MOVE && message2 != null) {
      return makeMove(message2, player2, player1);
    } else if (currentPlayer == 0 && state == TurnState.AWAITING_MOVE && message2 != null) {
      return makeMove(message2, player1, player2);
    } else if (currentPlayer == 1 && state == TurnState.AWAITING_MOVE && message1 != null) {
      return makeMove(message1, player2, player1);
    }
    return state;
  }

  private void notifyPlayerReady(int currentPlayer) {
    if (currentPlayer == 0) {
      player1.sendMessage(String.valueOf(TurnState.KEEP_TURN.ordinal()));
    } else {
      player2.sendMessage(String.valueOf(TurnState.KEEP_TURN.ordinal()));
    }
  }

  private void sendMessageToPlayers() {
    player1.sendMessage(String.valueOf(TurnState.END_GAME.ordinal()));
    player2.sendMessage(String.valueOf(TurnState.END_GAME.ordinal()));
  }

  private void closeConnections() {
    player1.closeConnections();
    player2.closeConnections();
  }

  private TurnState makeMove(MessageController message, ConnectionHandler sender, ConnectionHandler receiver) {
    return switch (message.getMessageType()) {
      case STEP -> {
        receiver.sendMessage(message.toString());
        yield TurnState.AWAITING_MOVE;
      }
      case GAME_OVER -> {
        sender.sendMessage(message.toString());
        yield TurnState.END_GAME;
      }
      case RESPONSE -> {
        sender.sendMessage(message.toString());
        yield GameEvent.containsOnlyMissed(message.getGameEvents()) ? TurnState.SWITCH_TURN : TurnState.KEEP_TURN;
      }
      case HEARTBEAT -> TurnState.CONNECTION_CHECK;
      default -> TurnState.ERROR;
    };
  }

  private enum TurnState {
    AWAITING_MOVE,
    SWITCH_TURN,
    INVALID_MOVE,
    KEEP_TURN,
    END_GAME,
    ERROR,
    CONNECTION_CHECK,
  }

  public void startGameLoop() {
    try {
      setupGame();

      int currentPlayer = 0;
      TurnState state = TurnState.INVALID_MOVE;

      while (state != TurnState.END_GAME || !player1.isClosed() || !player2.isClosed()) {
        MessageController message1 = retrieveHeartbeatOrMessage(player1, 1);
        MessageController message2 = retrieveHeartbeatOrMessage(player2, 2);

        validateConnectionTimeout();

        state = handleGameState(currentPlayer, state, message1, message2);

        if (state == TurnState.SWITCH_TURN) {
          currentPlayer = (currentPlayer + 1) % 2;
          state = TurnState.KEEP_TURN;
        }

        if (state == TurnState.KEEP_TURN) {
          notifyPlayerReady(currentPlayer);
          state = TurnState.INVALID_MOVE;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      closeConnections();
    }
  }
}
