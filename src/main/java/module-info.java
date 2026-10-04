module com.mycompany.bibliotecauni {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.mycompany.bibliotecauni to javafx.fxml;
    exports com.mycompany.bibliotecauni;
}
