module com.mycompany.bibliotecasistema {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.mycompany.bibliotecasistema.controller to javafx.fxml;

    exports com.mycompany.bibliotecasistema;
}
