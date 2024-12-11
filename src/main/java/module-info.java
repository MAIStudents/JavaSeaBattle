module JavaSeaBattle {
  requires javafx.controls;
  requires javafx.fxml;
  requires javafx.media;
  requires jlayer;
  requires java.desktop;

  exports ru.mai.lessons.rpks.Server;
  opens ru.mai.lessons.rpks.Server to javafx.fxml;
  exports ru.mai.lessons.rpks.controllers;
  opens ru.mai.lessons.rpks.controllers to javafx.fxml;
  exports ru.mai.lessons.rpks.include;
  opens ru.mai.lessons.rpks.include to javafx.fxml;
  exports ru.mai.lessons.rpks.Client;
  opens ru.mai.lessons.rpks.Client to javafx.fxml;
}