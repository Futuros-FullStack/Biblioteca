package com.mycompany.bibliotecasistema.controller;

import com.mycompany.bibliotecasistema.App;
import com.mycompany.bibliotecasistema.dao.EjemplarDAO;
import com.mycompany.bibliotecasistema.dao.LibroDAO;
import com.mycompany.bibliotecasistema.dao.NotificacionDAO;
import com.mycompany.bibliotecasistema.dao.PrestamoDAO;
import com.mycompany.bibliotecasistema.dao.UsuarioDAO;
import com.mycompany.bibliotecasistema.model.DetallePrestamo;
import com.mycompany.bibliotecasistema.model.Ejemplar;
import com.mycompany.bibliotecasistema.model.EstadoEjemplar;
import com.mycompany.bibliotecasistema.model.EstadoPrestamo;
import com.mycompany.bibliotecasistema.model.Libro;
import com.mycompany.bibliotecasistema.model.Notificacion;
import com.mycompany.bibliotecasistema.model.Usuario;
import com.mycompany.bibliotecasistema.util.Sesion;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

public class PrestamosController {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final PseudoClass VENCIDO = PseudoClass.getPseudoClass("vencido");
    private static final String SIN_ENTREGAS = "No hay préstamos pendientes.";
    private static final String SIN_ALUMNOS = "No hay alumnos registrados.";
    private static final String SIN_LIBROS = "No se encontraron libros.";
    private static final String ERROR_CARGA = "No se pudo cargar la información.";
    private static final String AVISO_SIN_ENVIAR = "SIN_ENVIAR";
    private static final String AVISO_PENDIENTE = "PENDIENTE";
    private static final String AVISO_EXITOSO = "EXITOSO";
    private static final String AVISO_FALLIDO = "FALLIDO";

    private final PrestamoDAO prestamoDAO = new PrestamoDAO();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final EjemplarDAO ejemplarDAO = new EjemplarDAO();
    private final LibroDAO libroDAO = new LibroDAO();
    private final NotificacionDAO notificacionDAO = new NotificacionDAO();

    private Map<String, Notificacion> avisos = Map.of();

    private Usuario alumno;

    @FXML
    private ToggleButton tabEntregas;
    @FXML
    private ToggleButton tabAlumnos;
    @FXML
    private VBox panelEntregas;
    @FXML
    private VBox panelAlumnos;
    @FXML
    private TextField txtBuscarEntregas;
    @FXML
    private TextField txtBuscarAlumno;
    @FXML
    private Label lblSinEntregas;
    @FXML
    private Label lblSinAlumnos;
    @FXML
    private TableView<DetallePrestamo> tablaEntregas;
    @FXML
    private TableColumn<DetallePrestamo, String> colAlumno;
    @FXML
    private TableColumn<DetallePrestamo, String> colLibro;
    @FXML
    private TableColumn<DetallePrestamo, String> colEntrega;
    @FXML
    private TableColumn<DetallePrestamo, String> colEstado;
    @FXML
    private TableColumn<DetallePrestamo, String> colAccionEntrega;
    @FXML
    private TableColumn<DetallePrestamo, String> colAviso;
    @FXML
    private TableColumn<DetallePrestamo, String> colAvisoAccion;
    @FXML
    private TableView<Usuario> tablaAlumnos;
    @FXML
    private TableColumn<Usuario, String> colMatricula;
    @FXML
    private TableColumn<Usuario, String> colNombre;
    @FXML
    private TableColumn<Usuario, String> colActivos;
    @FXML
    private TableColumn<Usuario, String> colTotal;
    @FXML
    private TableColumn<Usuario, String> colSituacion;
    @FXML
    private TableColumn<Usuario, String> colAccionAlumno;

    @FXML
    private TextField txtNombre;
    @FXML
    private TextField txtMatricula;
    @FXML
    private Button btnBuscarLibro;
    @FXML
    private Label lblLibroElegido;
    @FXML
    private DatePicker dpPrestamo;
    @FXML
    private DatePicker dpEntrega;
    @FXML
    private Button btnRegistrar;

    @FXML
    private TextField txtBuscarLibro;
    @FXML
    private Label lblSinLibros;
    @FXML
    private TableView<Libro> tablaLibros;
    @FXML
    private TableColumn<Libro, String> colTitulo;
    @FXML
    private TableColumn<Libro, String> colAutor;
    @FXML
    private TableColumn<Libro, String> colIsbn;
    @FXML
    private TableColumn<Libro, String> colDisponibles;
    @FXML
    private ComboBox<Ejemplar> cbCopia;

    @FXML
    private Label lblError;
    private Ejemplar ejemplarElegido;
    private Set<String> titulosEnPoder = Set.of();

    @FXML
    private void initialize() {
        
        if (tabEntregas == null) {
            return;
        }

        ToggleGroup pestanas = new ToggleGroup();
        tabEntregas.setToggleGroup(pestanas);
        tabAlumnos.setToggleGroup(pestanas);
        pestanas.selectedToggleProperty().addListener((obs, anterior, actual) -> {
            if (actual == null) {
                pestanas.selectToggle(anterior);
            } else {
                panelEntregas.setVisible(actual == tabEntregas);
                panelAlumnos.setVisible(actual == tabAlumnos);
            }
        });

        colAlumno.setCellValueFactory(fila -> new SimpleStringProperty(
                fila.getValue().getPrestamo().getAlumno().getNombre()));
        colLibro.setCellValueFactory(fila -> new SimpleStringProperty(
                fila.getValue().getEjemplar().getLibro().getTitulo()
                + " · " + fila.getValue().getEjemplar().getCodigoBarras()));
        colEntrega.setCellValueFactory(fila -> new SimpleStringProperty(
                fila.getValue().getPrestamo().getFechaEntregaEsperada().format(FORMATO_FECHA)));
        colEstado.setCellValueFactory(fila -> new SimpleStringProperty(textoEstado(fila.getValue())));
        tablaEntregas.setRowFactory(tabla -> new TableRow<DetallePrestamo>() {
            @Override
            protected void updateItem(DetallePrestamo detalle, boolean vacia) {
                super.updateItem(detalle, vacia);
                pseudoClassStateChanged(VENCIDO, !vacia && detalle != null && diasDeAtraso(detalle) > 0);
            }
        });
        colAccionEntrega.setCellFactory(c -> new TableCell<DetallePrestamo, String>() {
            @Override
            protected void updateItem(String valor, boolean vacia) {
                super.updateItem(valor, vacia);
                if (vacia) {
                    setGraphic(null);
                    return;
                }
                DetallePrestamo detalle = getTableView().getItems().get(getIndex());
                Button devolver = new Button("Registrar devolución");
                devolver.getStyleClass().add("boton-tabla");
                devolver.setOnAction(e -> registrarDevolucion(detalle));
                HBox acciones = new HBox(6, devolver);
                acciones.setAlignment(Pos.CENTER_RIGHT);
                if (puedeRenovar(detalle)) {
                    Button renovar = new Button("Renovar");
                    renovar.getStyleClass().add("boton-tabla-secundario");
                    renovar.setOnAction(e -> renovarPrestamo(detalle));
                    acciones.getChildren().add(0, renovar);
                }
                if (puedeAnular(detalle)) {
                    Button anular = new Button("Anular");
                    anular.getStyleClass().add("boton-tabla-secundario");
                    anular.setOnAction(e -> anularPrestamo(detalle));
                    acciones.getChildren().add(0, anular);
                }
                setGraphic(acciones);
            }
        });

       
        colAviso.setCellValueFactory(fila -> new SimpleStringProperty(estadoAviso(fila.getValue())));
        colAviso.setCellFactory(c -> new TableCell<DetallePrestamo, String>() {
            @Override
            protected void updateItem(String estado, boolean vacia) {
                super.updateItem(estado, vacia);
                if (vacia || estado == null) {
                    setGraphic(null);
                    return;
                }
                Label etiqueta = crearEtiquetaAviso(estado);
                if (AVISO_FALLIDO.equals(estado)) {
                    etiqueta.setStyle("-fx-cursor: hand;");
                    DetallePrestamo detalle = getTableView().getItems().get(getIndex());
                    etiqueta.setOnMouseClicked(e -> verDetalleAviso(detalle));
                }
                setGraphic(etiqueta);
            }
        });
        colAvisoAccion.setCellValueFactory(fila -> new SimpleStringProperty(estadoAviso(fila.getValue())));
        colAvisoAccion.setCellFactory(c -> new TableCell<DetallePrestamo, String>() {
            @Override
            protected void updateItem(String estado, boolean vacia) {
                super.updateItem(estado, vacia);
                setGraphic(null);
                if (vacia || estado == null) {
                    return;
                }
               
                if (AVISO_SIN_ENVIAR.equals(estado)) {
                    Button notificar = new Button("Notificar");
                    notificar.getStyleClass().add("boton-tabla-secundario");
                    notificar.setDisable(true);
                    setGraphic(notificar);
                } else if (AVISO_FALLIDO.equals(estado)) {
                    DetallePrestamo detalle = getTableView().getItems().get(getIndex());
                    Button reenviar = new Button("Reenviar");
                    reenviar.getStyleClass().add("boton-tabla-secundario");
                    reenviar.setOnAction(e -> reenviarAviso(detalle));
                    setGraphic(reenviar);
                }
            }
        });

        // Tabla Alumnos
        colMatricula.setCellValueFactory(fila -> new SimpleStringProperty(fila.getValue().getMatricula()));
        colNombre.setCellValueFactory(fila -> new SimpleStringProperty(fila.getValue().getNombre()));
        colActivos.setCellValueFactory(fila -> new SimpleStringProperty(
                String.valueOf(fila.getValue().getPrestamosActivos())));
        colTotal.setCellValueFactory(fila -> new SimpleStringProperty(
                String.valueOf(fila.getValue().getPrestamosTotal())));
        colSituacion.setCellValueFactory(fila -> new SimpleStringProperty(
                fila.getValue().isBloqueado() ? "Bloqueado" : "Al corriente"));
        colAccionAlumno.setCellFactory(c -> new TableCell<Usuario, String>() {
            @Override
            protected void updateItem(String valor, boolean vacia) {
                super.updateItem(valor, vacia);
                setGraphic(vacia ? null : crearAccionesAlumno(getTableView().getItems().get(getIndex())));
            }
        });

       
        txtBuscarEntregas.textProperty().addListener((obs, anterior, texto) -> cargarEntregas());
        txtBuscarAlumno.textProperty().addListener((obs, anterior, texto) -> cargarAlumnos());

        cargarEntregas();
        cargarAlumnos();
    }

    @FXML
    private void nuevoPrestamo() {
        tabAlumnos.setSelected(true);
        Alert guia = new Alert(Alert.AlertType.INFORMATION,
                "Para registrar un préstamo:\n\n"
                + "1. Escribe el nombre o la matrícula del alumno en el buscador.\n"
                + "2. Presiona «Prestar / ver ›» en su renglón.\n"
                + "3. En su ficha, presiona «Realizar préstamo» y elige el libro y el ejemplar.\n\n"
                + "¿No aparece? Regístralo primero con «+ Registrar alumno nuevo».",
                ButtonType.OK);
        guia.initOwner(tablaAlumnos.getScene().getWindow());
        guia.setTitle("Nuevo préstamo");
        guia.setHeaderText(null);
        guia.showAndWait();
        txtBuscarAlumno.requestFocus();
    }

    @FXML
    private void registrarAlumno() {
        App.abrirVentana("registrar-alumno", tablaAlumnos);
        recargar();
    }

    private String estadoAviso(DetallePrestamo detalle) {
        Notificacion aviso = avisos.get(claveAviso(detalle));
        return aviso == null ? AVISO_SIN_ENVIAR : aviso.getEstado().getNombreEstadoNoti();
    }

    private Label crearEtiquetaAviso(String estado) {
        String texto;
        String estilo;
        switch (estado) {
            case AVISO_PENDIENTE:
                texto = "Pendiente";
                estilo = "estado-prestado";
                break;
            case AVISO_EXITOSO:
                texto = "Notificado";
                estilo = "estado-disponible";
                break;
            case AVISO_FALLIDO:
                texto = "Envío fallido";
                estilo = "estado-vencido";
                break;
            default:
                texto = "Sin enviar";
                estilo = "estado-reparacion";
        }
        Region punto = new Region();
        punto.getStyleClass().add("punto");
        Label etiqueta = new Label(texto, punto);
        etiqueta.getStyleClass().addAll("estado", estilo);
        etiqueta.setGraphicTextGap(7);
        return etiqueta;
    }

    private String claveAviso(DetallePrestamo detalle) {
        return detalle.getPrestamo().getIdPrestamo() + "|" + detalle.getEjemplar().getIdEjemplar();
    }

    private void verDetalleAviso(DetallePrestamo detalle) {
        Notificacion aviso = avisos.get(claveAviso(detalle));
        if (aviso != null) {
            avisar(Alert.AlertType.INFORMATION, "No se pudo enviar el aviso: " + aviso.getDetalleError());
        }
    }


    private void reenviarAviso(DetallePrestamo detalle) {
        Notificacion aviso = avisos.get(claveAviso(detalle));
        if (aviso == null) {
            return;
        }
        try {
            notificacionDAO.reenviar(aviso.getIdNotificacion());
            cargarEntregas();
            avisar(Alert.AlertType.INFORMATION, "El aviso quedó pendiente y se enviará en la siguiente ejecución del proceso de envío.");
        } catch (SQLException e) {
            avisar(Alert.AlertType.ERROR, "No se pudo reenviar el aviso.");
        }
    }


    private boolean puedeRenovar(DetallePrestamo detalle) {
        return EstadoPrestamo.ACTIVO.equals(detalle.getPrestamo().getEstado().getNombreEstadoPrestamo())
                && !detalle.getPrestamo().isRenovado()
                && !detalle.getPrestamo().getFechaEntregaEsperada().isBefore(LocalDate.now());
    }

    private void renovarPrestamo(DetallePrestamo detalle) {
        Alert confirmar = new Alert(Alert.AlertType.CONFIRMATION,
                "Se sumarán 2 días hábiles a la entrega de «" + detalle.getEjemplar().getLibro().getTitulo()
                + "». Solo se puede renovar una vez. ¿Continuar?", ButtonType.OK, ButtonType.CANCEL);
        confirmar.initOwner(tablaEntregas.getScene().getWindow());
        confirmar.setHeaderText(null);
        if (confirmar.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        try {
            prestamoDAO.renovar(detalle.getPrestamo().getIdPrestamo(), detalle.getEjemplar().getIdEjemplar());
            recargar();
        } catch (SQLException e) {
            
            String mensaje = e.getMessage() == null ? "No se pudo renovar el préstamo."
                    : e.getMessage().lines().findFirst().get().replaceFirst("^ERROR:\\s*", "");
            avisar(Alert.AlertType.WARNING, mensaje);
        }
    }

   
    private boolean puedeAnular(DetallePrestamo detalle) {
        return EstadoPrestamo.ACTIVO.equals(detalle.getPrestamo().getEstado().getNombreEstadoPrestamo())
                && detalle.getPrestamo().getFechaPrestamo().toLocalDate().equals(LocalDate.now());
    }

    private void anularPrestamo(DetallePrestamo detalle) {
        Alert confirmar = new Alert(Alert.AlertType.CONFIRMATION,
                "Se anulará todo el préstamo de " + detalle.getPrestamo().getAlumno().getNombre()
                + " y sus ejemplares volverán a estar disponibles. ¿Continuar?", ButtonType.OK, ButtonType.CANCEL);
        confirmar.initOwner(tablaEntregas.getScene().getWindow());
        confirmar.setHeaderText(null);
        if (confirmar.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        try {
            prestamoDAO.anular(detalle.getPrestamo().getIdPrestamo());
            recargar();
        } catch (SQLException e) {
            String mensaje = e.getMessage() == null ? "No se pudo anular el préstamo."
                    : e.getMessage().lines().findFirst().get().replaceFirst("^ERROR:\\s*", "");
            avisar(Alert.AlertType.WARNING, mensaje);
        }
    }

    private void registrarDevolucion(DetallePrestamo detalle) {
        FXMLLoader loader = App.prepararVentana("devolucion");
        DevolucionController ventana = loader.getController();
        ventana.prepararDevolucion(detalle);
        App.mostrarVentana(loader, tablaEntregas);
        recargar();
    }

    private HBox crearAccionesAlumno(Usuario alumno) {
        Button ver = new Button("Prestar / ver  ›");
        ver.getStyleClass().add("boton-tabla");
        ver.setOnAction(e -> verAlumno(alumno));

        Button editar = EjemplaresLibroController.botonIcono("icono-editar");
        editar.setOnAction(e -> editarAlumno(alumno));

        Button eliminar = EjemplaresLibroController.botonIcono("icono-eliminar");
        eliminar.getStyleClass().add("peligro");
        eliminar.setOnAction(e -> eliminarAlumno(alumno));

        HBox acciones = new HBox(6, ver, editar, eliminar);
        acciones.setAlignment(Pos.CENTER_RIGHT);
        return acciones;
    }

    private void editarAlumno(Usuario alumno) {
        FXMLLoader loader = App.prepararVentana("registrar-alumno");
        RegistrarAlumnoController ventana = loader.getController();
        ventana.botonEditar(alumno);
        App.mostrarVentana(loader, tablaAlumnos);
        recargar();
    }

   
    private void eliminarAlumno(Usuario alumno) {
        if (alumno.getPrestamosActivos() > 0) {
            avisar(Alert.AlertType.WARNING, "No se puede eliminar a " + alumno.getNombre()
                    + " porque tiene préstamos pendientes. Registra primero su devolución.");
            return;
        }
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION,
                "Se eliminará a " + alumno.getNombre() + " (matrícula " + alumno.getMatricula() + "). ¿Continuar?",
                ButtonType.OK, ButtonType.CANCEL);
        alerta.initOwner(tablaAlumnos.getScene().getWindow());
        alerta.setHeaderText(null);
        if (alerta.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        try {
            usuarioDAO.eliminarAlumno(alumno.getIdUsuario());
            recargar();
        } catch (SQLException e) {
            if ("23503".equals(e.getSQLState())) {
                avisar(Alert.AlertType.WARNING, "No se puede eliminar a " + alumno.getNombre()
                        + " porque tiene historial de préstamos.");
            } else {
                avisar(Alert.AlertType.ERROR, "No se pudo eliminar al alumno.");
            }
        }
    }

    private void avisar(Alert.AlertType tipo, String mensaje) {
        Alert alerta = new Alert(tipo, mensaje);
        alerta.initOwner(tablaAlumnos.getScene().getWindow());
        alerta.setHeaderText(null);
        alerta.showAndWait();
    }

    private void verAlumno(Usuario alumno) {
        FXMLLoader loader = App.prepararVentana("detalle-alumno");
        DetalleAlumnoController detalle = loader.getController();
        detalle.setAlumno(alumno);
        App.mostrarVentana(loader, tablaAlumnos);
        recargar();
    }

    private void recargar() {
        cargarEntregas();
        cargarAlumnos();
    }

    //Ventana Nuevo préstamo

    public void prepararNuevoPrestamo(Usuario alumno) {
        this.alumno = alumno;
        txtNombre.setText(alumno.getNombre());
        txtMatricula.setText(alumno.getMatricula());
        dpPrestamo.setValue(LocalDate.now());
        ejecutar(prestamoDAO::fechaEntregaPrevista, dpEntrega::setValue, error -> mostrarError(ERROR_CARGA));
    }

    @FXML
    private void buscarLibro() {
        FXMLLoader loader = App.prepararVentana("buscar-libro");
        PrestamosController busqueda = loader.getController();
        busqueda.prepararBusqueda(alumno);
        App.mostrarVentana(loader, btnBuscarLibro);

        if (busqueda.ejemplarElegido != null) {
            ejemplarElegido = busqueda.ejemplarElegido;
            lblLibroElegido.setText(ejemplarElegido.getLibro().getTitulo() + " · " + textoCopia(ejemplarElegido));
            ocultarError();
        }
    }

    @FXML
    private void registrarPrestamo() {
        if (ejemplarElegido == null) {
            mostrarError("Busca y elige el libro que se llevará.");
            return;
        }
        btnRegistrar.setDisable(true);
        ejecutar(() -> {
            prestamoDAO.registrar(alumno.getMatricula(), ejemplarElegido.getCodigoBarras(),
                    Sesion.getAdministrador().getIdUsuario(), null);
            return null;
        }, resultado -> App.cerrar(txtNombre), error -> {
            btnRegistrar.setDisable(false);
            mostrarError(mensajeDeError(error));
        });
    }

    @FXML
    private void cerrar(ActionEvent evento) {
        App.cerrar((Node) evento.getSource());
    }

    //Ventana Buscar libro

    public void prepararBusqueda(Usuario alumno) {
        colTitulo.setCellValueFactory(fila -> new SimpleStringProperty(fila.getValue().getTitulo()));
        colAutor.setCellValueFactory(fila -> new SimpleStringProperty(fila.getValue().getAutoresTexto()));
        colIsbn.setCellValueFactory(fila -> new SimpleStringProperty(fila.getValue().getIsbn()));
        colDisponibles.setCellValueFactory(fila -> new SimpleStringProperty(
                String.valueOf(fila.getValue().getCopiasDisponibles())));
        cbCopia.setConverter(new StringConverter<Ejemplar>() {
            @Override
            public String toString(Ejemplar ejemplar) {
                return ejemplar == null ? "" : textoCopia(ejemplar);
            }

            @Override
            public Ejemplar fromString(String texto) {
                return null;
            }
        });
        tablaLibros.getSelectionModel().selectedItemProperty().addListener((obs, anterior, libro) -> cargarCopias(libro));
        txtBuscarLibro.textProperty().addListener((obs, anterior, texto) -> buscarLibros());

        ejecutar(() -> prestamoDAO.listarEnPoder(alumno.getIdUsuario()), enPoder -> {
            titulosEnPoder = enPoder.stream()
                    .map(detalle -> detalle.getEjemplar().getLibro().getTitulo())
                    .collect(Collectors.toSet());
            buscarLibros();
        }, error -> lblSinLibros.setText(ERROR_CARGA));
    }

    private void buscarLibros() {
        String busqueda = txtBuscarLibro.getText().trim();
        ejecutar(() -> libroDAO.buscarCatalogo(busqueda), libros -> {
            if (!busqueda.equals(txtBuscarLibro.getText().trim())) {
                return;
            }
            lblSinLibros.setText(SIN_LIBROS);
            tablaLibros.setItems(FXCollections.observableArrayList(libros.stream()
                    .filter(libro -> !titulosEnPoder.contains(libro.getTitulo()))
                    .collect(Collectors.toList())));
        }, error -> {
            lblSinLibros.setText(ERROR_CARGA);
            tablaLibros.getItems().clear();
        });
    }

    private void cargarCopias(Libro libro) {
        cbCopia.getItems().clear();
        ocultarError();
        if (libro == null) {
            return;
        }
        ejecutar(() -> ejemplarDAO.listarPorLibro(libro.getIdLibro()), ejemplares -> {
            if (tablaLibros.getSelectionModel().getSelectedItem() != libro) {
                return;
            }
            List<Ejemplar> disponibles = ejemplares.stream()
                    .filter(e -> EstadoEjemplar.DISPONIBLE.equals(e.getEstado().getNombreEstadoEjemplar()))
                    .collect(Collectors.toList());
            cbCopia.getItems().setAll(disponibles);
            if (disponibles.size() == 1) {
                cbCopia.getSelectionModel().selectFirst();
            }
        }, error -> mostrarError(ERROR_CARGA));
    }

    @FXML
    private void seleccionarLibro() {
        if (tablaLibros.getSelectionModel().getSelectedItem() == null) {
            mostrarError("Elige un libro de la tabla.");
            return;
        }
        if (cbCopia.getValue() == null) {
            mostrarError(cbCopia.getItems().isEmpty()
                    ? "Este libro no tiene ejemplares disponibles."
                    : "Elige el ejemplar que se llevará.");
            return;
        }
        ejemplarElegido = cbCopia.getValue();
        App.cerrar(txtBuscarLibro);
    }

    private String textoCopia(Ejemplar ejemplar) {
        return ejemplar.getCodigoBarras() + " · " + ejemplar.getUbicacion();
    }
    
    private String mensajeDeError(Throwable error) {
        if (error instanceof SQLException && error.getMessage() != null) {
            return error.getMessage().replaceFirst("^ERROR: ", "").split("\n")[0];
        }
        return "No se pudo registrar el préstamo.";
    }

    private void mostrarError(String mensaje) {
        lblError.setText(mensaje);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    private void ocultarError() {
        lblError.setVisible(false);
        lblError.setManaged(false);
    }

    private void cargarEntregas() {
        String busqueda = txtBuscarEntregas.getText().trim();
        ejecutar(() -> new SimpleEntry<>(prestamoDAO.listarProximasEntregas(busqueda), notificacionDAO.listarVigentes()), resultado -> {
            
            if (!busqueda.equals(txtBuscarEntregas.getText().trim())) {
                return;
            }
            lblSinEntregas.setText(SIN_ENTREGAS);
            avisos = resultado.getValue();
            tablaEntregas.setItems(FXCollections.observableArrayList(resultado.getKey()));
            tablaEntregas.refresh();
        }, error -> {
            lblSinEntregas.setText(ERROR_CARGA);
            tablaEntregas.getItems().clear();
        });
    }

    private void cargarAlumnos() {
        String busqueda = txtBuscarAlumno.getText().trim();
        ejecutar(() -> usuarioDAO.buscarAlumnos(busqueda), lista -> {
            if (!busqueda.equals(txtBuscarAlumno.getText().trim())) {
                return;
            }
            lblSinAlumnos.setText(SIN_ALUMNOS);
            tablaAlumnos.setItems(FXCollections.observableArrayList(lista));
            tablaAlumnos.refresh();
        }, error -> {
            lblSinAlumnos.setText(ERROR_CARGA);
            tablaAlumnos.getItems().clear();
        });
    }

    private <T> void ejecutar(Callable<T> consulta, Consumer<T> alTerminar, Consumer<Throwable> alFallar) {
        T resultado;
        try {
            resultado = consulta.call();
        } catch (Exception e) {
            alFallar.accept(e);
            return;
        }
        alTerminar.accept(resultado);
    }

    private long diasDeAtraso(DetallePrestamo detalle) {
        return ChronoUnit.DAYS.between(detalle.getPrestamo().getFechaEntregaEsperada(), LocalDate.now());
    }

    private String textoEstado(DetallePrestamo detalle) {
        long dias = diasDeAtraso(detalle);
        if (dias <= 0) {
            return "Activo";
        }
        return "Vencido hace " + dias + (dias == 1 ? " día" : " días");
    }

    private static <S> void columnaBoton(TableColumn<S, String> columna, String texto, Consumer<S> accion) {
        columna.setCellFactory(c -> new TableCell<S, String>() {
            @Override
            protected void updateItem(String valor, boolean vacia) {
                super.updateItem(valor, vacia);
                if (vacia) {
                    setGraphic(null);
                } else {
                    Button boton = new Button(texto);
                    boton.getStyleClass().add("boton-tabla");
                    boton.setOnAction(e -> accion.accept(getTableView().getItems().get(getIndex())));
                    setGraphic(boton);
                }
            }
        });
    }
}
