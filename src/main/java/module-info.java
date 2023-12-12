module ru.mai.lessons.rpks.javaseabattle {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires javafx.graphics;

    requires log4j;
    requires javafx.swing;


    opens ru.mai.lessons.rpks.javaseabattle to javafx.fxml;
    exports ru.mai.lessons.rpks.javaseabattle;
}