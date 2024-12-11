package ru.mai.lessons.rpks.Server.include;

import ru.mai.lessons.rpks.controllers.MessageController;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ConnectionHandler {
  private final Socket player;
  private final PrintWriter printWriter;
  private final BufferedReader reader;
  private final BlockingQueue<MessageController> messageQueue = new LinkedBlockingQueue<>();
  private final String playerName;

  public ConnectionHandler(Socket player, String playerName) throws IOException {
    this.player = player;
    this.printWriter = new PrintWriter(player.getOutputStream(), true);
    this.reader = new BufferedReader(new InputStreamReader(player.getInputStream()));
    this.playerName = playerName;
  }

  public void startMonitoring() {
    new Thread(() -> {
      try {
        int counter = 0;
        while (!player.isClosed()) {
          if (reader.ready()) {
            MessageController message = MessageController.parseFromRawMessage(reader.readLine());
            if ("EXIT".equals(message.getMessageContent())) {
              closeConnections();
              throw new RuntimeException("The player has left");
            }
            messageQueue.put(message);
          }
          Thread.sleep(100);
          if (counter % 30 == 0) {
            sendHeartbeat();
          }
          counter++;
        }
      } catch (Exception e) {
        System.out.println(playerName + " switched off: " + e.getMessage());
        closeConnections();
      }
    }).start();
  }

  public MessageController getNextMessage() {
    return messageQueue.poll();
  }

  public void sendMessage(String message) {
    printWriter.write(message + "\n");
    printWriter.flush();
  }

  public void sendHeartbeat() {
    sendMessage("0");
  }

  public void closeConnections() {
    try {
      if (player != null) player.close();
      if (printWriter != null) printWriter.close();
      if (reader != null) reader.close();
    } catch (IOException e) {
      System.out.println("Error closing connections: " + e.getMessage());
    }
  }

  public boolean isClosed() {
    return player.isClosed();
  }
}
