module ru.mai.lessons.rpks.javaseabattle {
    requires javafx.controls;
    requires javafx.fxml;

    opens ru.mai.lessons.rpks.javaseabattle.utils;
    exports ru.mai.lessons.rpks.javaseabattle.utils;

    opens ru.mai.lessons.rpks.javaseabattle.server_response_handler;
    exports ru.mai.lessons.rpks.javaseabattle.server_response_handler;

    opens ru.mai.lessons.rpks.javaseabattle.controller.game_control_button_state;
    exports ru.mai.lessons.rpks.javaseabattle.controller.game_control_button_state;

    opens ru.mai.lessons.rpks.javaseabattle to javafx.fxml;
    exports ru.mai.lessons.rpks.javaseabattle;
    exports ru.mai.lessons.rpks.javaseabattle.controller;
    opens ru.mai.lessons.rpks.javaseabattle.controller to javafx.fxml;
}