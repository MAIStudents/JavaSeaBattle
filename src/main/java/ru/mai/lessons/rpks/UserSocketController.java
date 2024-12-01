package ru.mai.lessons.rpks;

import java.io.IOException;
import java.net.Socket;

public final class UserSocketController {
  private static Socket socket;
  private static UserSocketController instance;

  public static UserSocketController getInstance() {
    if (instance == null) {
      instance = new UserSocketController();
    }
    return instance;
  }

  public void start(final Socket ss) {
    socket = ss;
  }

  public void stop() {
    try {
      if (socket != null && !socket.isClosed()) {
        socket.close();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
