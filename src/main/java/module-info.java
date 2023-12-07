module JavaSeaBattle {
    requires javafx.controls;
    requires javafx.fxml;
    requires apache.log4j.extras;
    requires javatuples;


    exports ru.mai.lessons.rpks.server;
    opens ru.mai.lessons.rpks.server to javafx.fxml;
    exports ru.mai.lessons.rpks.client;
    opens ru.mai.lessons.rpks.client to javafx.fxml;
    exports ru.mai.lessons.rpks.client_handler;
    opens ru.mai.lessons.rpks.client_handler to javafx.fxml;
    exports ru.mai.lessons.rpks.application;
    opens ru.mai.lessons.rpks.application to javafx.fxml;
    exports ru.mai.lessons.rpks.message;
    opens ru.mai.lessons.rpks.fill_grid to javafx.fxml;
    exports ru.mai.lessons.rpks.fill_grid;
    exports ru.mai.lessons.rpks.battle_grid;
    exports ru.mai.lessons.rpks.functional_interface;
}