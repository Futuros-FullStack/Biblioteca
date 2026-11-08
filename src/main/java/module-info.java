module com.mycompany.bibliotecasistema {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires org.postgresql.jdbc;
    requires jbcrypt;
    requires jakarta.mail;

    opens com.mycompany.bibliotecasistema.controller to javafx.fxml;
    opens com.mycompany.bibliotecasistema.model to javafx.base;

    exports com.mycompany.bibliotecasistema;
}
