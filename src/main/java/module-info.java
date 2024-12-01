module ru.mai.lessons.rpks {
  requires javafx.controls;
  requires javafx.fxml;
  requires java.desktop;

  opens ru.mai.lessons.rpks to javafx.fxml;
  exports ru.mai.lessons.rpks;
}