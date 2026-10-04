module com.mycompany.bibliotecasistema {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.mycompany.bibliotecasistema.controller to javafx.fxml;
    opens com.mycompany.bibliotecasistema.model to javafx.fxml, javafx.base;

    exports com.mycompany.bibliotecasistema;
}
