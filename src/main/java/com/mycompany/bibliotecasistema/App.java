package com.mycompany.bibliotecasistema;

import com.mycompany.bibliotecasistema.util.Navegador;
import com.mycompany.bibliotecasistema.util.Vista;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Punto de entrada: abre la aplicación en el Login.
 */
public class App extends Application {

    private static final double ANCHO_MINIMO = 1100;
    private static final double ALTO_MINIMO = 650;

    @Override
    public void start(Stage stage) {
        Navegador.iniciar(stage, Vista.LOGIN, ANCHO_MINIMO, ALTO_MINIMO);
        stage.setTitle("Biblioteca - Facultad de Negocios");
        stage.setMinWidth(ANCHO_MINIMO);
        stage.setMinHeight(ALTO_MINIMO);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
