package ru.mai.lessons.rpks;

import java.io.PrintWriter;

/**
 * Утилитный класс для общих методов, используемых между Сокетами.
 */
public final class Common {

  /**
   * Отправляет сообщение через указанный поток вывода.
   *
   * @param message сообщение, которое нужно отправить
   * @param out поток вывода, через который будет отправлено сообщение
   */
  public static void sendMessage(final String message,
                                 final PrintWriter out) {
    if (out != null) {
      out.println(message);
    } else {
      System.out.println("Output flow don't init.");
    }
  }
}
