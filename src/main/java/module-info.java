module ru.mai.lessons.rpks {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires javafx.graphics;

    requires log4j;
    requires javafx.swing;


    opens ru.mai.lessons.rpks to javafx.fxml;
    exports ru.mai.lessons.rpks;
    exports ru.mai.lessons.rpks.Server;
    opens ru.mai.lessons.rpks.Server to javafx.fxml;
    exports ru.mai.lessons.rpks.Client;
    opens ru.mai.lessons.rpks.Client to javafx.fxml;
    exports ru.mai.lessons.rpks.Components;
    opens ru.mai.lessons.rpks.Components to javafx.fxml;
}