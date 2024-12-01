package ru.mai.lessons.rpks;

import java.io.IOException;
import java.net.ServerSocket;

public final class ServerSocketController {
  private static ServerSocket serverSocket;
  private static ServerSocketController instance;
  private boolean running;

  public static ServerSocketController getInstance() {
    if (instance == null) {
      instance = new ServerSocketController();
    }
    return instance;
  }

  public void start(final ServerSocket ss) {
    running = true;
    serverSocket = ss;
  }

  public void stop() {
    running = false;

    try {
      if (serverSocket != null && !serverSocket.isClosed()) {
        serverSocket.close();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public boolean isRunning() {
    return running;
  }
}
