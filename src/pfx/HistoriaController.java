package pfx;

import bdpryfinal.HistoriaDaoJdbc;
import bdpryfinal.HistoriaService;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.text.Text;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;

public class HistoriaController {

    /* ========= FXML: raíz ========= */
    @FXML private TabPane tabPane;

    /* ========= TAB 1: Historia Clínica ========= */
    @FXML private TextField txtCodigoPaciente;
    @FXML private Label lblPaciente;
    @FXML private Label lblHistoriaId;

    /* ========= TAB 2: Notas ========= */
    @FXML private TextField txtCodigoPacienteNotas;
    @FXML private TableView<HistoriaDaoJdbc.Nota> tblNotas;
    @FXML private TableColumn<HistoriaDaoJdbc.Nota, String> colFecha;
    @FXML private TableColumn<HistoriaDaoJdbc.Nota, String> colTexto;     // mostrará motivo_consulta
    @FXML private TableColumn<HistoriaDaoJdbc.Nota, String> colMotivo;
    @FXML private TableColumn<HistoriaDaoJdbc.Nota, String> colAlergias;
    @FXML private TableColumn<HistoriaDaoJdbc.Nota, String> colMedic;
    @FXML private TableColumn<HistoriaDaoJdbc.Nota, String> colRecom;
    @FXML private Label lblHistoriaIdNotas;

    /* ========= Servicios/estado ========= */
    private final HistoriaService historiaService = new HistoriaService();
    private HistoriaService.HistoriaView vistaActual;

    private final DateTimeFormatter FECHA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /* ========= Ciclo de vida ========= */
    @FXML
    private void initialize() {
        // Estado por defecto en labels
        setHistoriaIdLabel(null);
        setPacienteLabel(null);
        setHistoriaIdNotasLabel(null);

        // Configuración de tabla Notas
        if (tblNotas != null) {
            setupNotasTable();
        }
    }

    private void setupNotasTable() {
        // Fecha
        if (colFecha != null) {
            colFecha.setCellValueFactory(cd ->
                new SimpleStringProperty(formatTs(cd.getValue().creadaEn)));
        }
        // OJO: ya no existe n.texto. Usamos motivo_consulta:
        setupWrappingColumn(colTexto,    n -> n.motivoConsulta);
        setupWrappingColumn(colMotivo,   n -> n.motivoConsulta);
        setupWrappingColumn(colAlergias, n -> n.alergias);
        setupWrappingColumn(colMedic,    n -> n.medicamentos);
        setupWrappingColumn(colRecom,    n -> n.recomendaciones);

        if (tblNotas.getPlaceholder() == null) {
            tblNotas.setPlaceholder(new Label("Sin notas para esta historia."));
        }
    }

    private void setupWrappingColumn(TableColumn<HistoriaDaoJdbc.Nota, String> col,
                                     java.util.function.Function<HistoriaDaoJdbc.Nota, String> getter) {
        if (col == null) return;

        col.setCellValueFactory(cd -> new SimpleStringProperty(safe(getter.apply(cd.getValue()))));

        col.setCellFactory(tc -> new TableCell<>() {
            private final Text text = new Text();
            {
                text.wrappingWidthProperty().bind(tc.widthProperty().subtract(12)); // margen
                setGraphic(text);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                text.setText(empty ? null : item);
            }
        });
    }

    /* ========= Handlers TAB 1 ========= */
    @FXML
    private void onAbrirHistoria(ActionEvent e) {
        final String codigo = txtCodigoPaciente != null ? txtCodigoPaciente.getText() : null;
        abrirHistoriaPorCodigo(codigo);
    }

    private void abrirHistoriaPorCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            warn("Código requerido", "Ingresa la identificación/cédula del paciente.");
            return;
        }
        try {
            vistaActual = historiaService.abrirPorCodigoPaciente(codigo.trim());

            // Actualizar TAB 1
            setHistoriaIdLabel(vistaActual.historiaId);
            setPacienteLabel(vistaActual.paciente != null ? vistaActual.paciente.nombre : null);

            // Sincronizar TAB 2
            if (txtCodigoPacienteNotas != null) {
                txtCodigoPacienteNotas.setText(
                    vistaActual.paciente != null ? vistaActual.paciente.identificacion : codigo.trim()
                );
            }
            if (lblHistoriaIdNotas != null) setHistoriaIdNotasLabel(vistaActual.historiaId);
            if (tblNotas != null && vistaActual.notas != null) {
                tblNotas.getItems().setAll(vistaActual.notas);
            }

            info("Historia abierta",
                "Historia #" + vistaActual.historiaId + " para " +
                    safe(vistaActual.paciente != null ? vistaActual.paciente.nombre : codigo.trim()));

        } catch (IllegalArgumentException ex) {
            warn("No encontrado", ex.getMessage());
        } catch (Exception ex) {
            error("Error al abrir/crear historia", exceptionChain(ex));
        }
    }

    /* ========= Handlers TAB 2 ========= */
    @FXML
    private void onCargarNotas(ActionEvent e) {
        if (txtCodigoPacienteNotas == null) {
            warn("Vista sin campo de código", "No hay campo 'Código Paciente' en esta pestaña.");
            return;
        }
        abrirHistoriaPorCodigo(txtCodigoPacienteNotas.getText());
        // (abrirHistoriaPorCodigo ya refresca la tabla)
    }

    @FXML
    private void onRefrescarNotas(ActionEvent e) {
        if (vistaActual == null) {
            warn("Sin historia abierta", "Carga primero la historia.");
            return;
        }
        final String codigo =
            (txtCodigoPacienteNotas != null && txtCodigoPacienteNotas.getText() != null && !txtCodigoPacienteNotas.getText().isBlank())
                ? txtCodigoPacienteNotas.getText().trim()
                : (vistaActual.paciente != null ? vistaActual.paciente.identificacion : null);

        if (codigo == null || codigo.isBlank()) {
            warn("Código requerido", "No se encontró código de paciente para refrescar.");
            return;
        }
        try {
            vistaActual = historiaService.abrirPorCodigoPaciente(codigo);
            if (tblNotas != null) tblNotas.getItems().setAll(vistaActual.notas);
            setHistoriaIdNotasLabel(vistaActual.historiaId);
            info("Refrescado", "Notas actualizadas para la historia #" + vistaActual.historiaId);
        } catch (Exception ex) {
            error("Error al refrescar", exceptionChain(ex));
        }
    }

    @FXML
    private void onAgregarNotaCompleta(ActionEvent e) {
        if (txtCodigoPacienteNotas == null) {
            warn("Vista sin campo de código", "No hay campo 'Código Paciente' en esta pestaña.");
            return;
        }
        final String codigo = txtCodigoPacienteNotas.getText();
        if (codigo == null || codigo.isBlank()) {
            warn("Código requerido", "Ingresa la identificación/cédula del paciente.");
            return;
        }

        // ---- Diálogo para capturar los campos de la nota (sin 'texto') ----
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Agregar nota");
        dlg.setHeaderText("Completa la información de la nota");

        ButtonType BTN_GUARDAR = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(BTN_GUARDAR, ButtonType.CANCEL);

        // Controles
        TextArea txtMotivo = new TextArea();
        txtMotivo.setPromptText("Motivo de consulta (obligatorio)");
        txtMotivo.setPrefRowCount(2);

        TextArea txtAlergias = new TextArea();
        txtAlergias.setPromptText("Alergias");
        txtAlergias.setPrefRowCount(2);

        TextArea txtMedic = new TextArea();
        txtMedic.setPromptText("Medicamentos");
        txtMedic.setPrefRowCount(2);

        TextArea txtRecom = new TextArea();
        txtRecom.setPromptText("Recomendaciones");
        txtRecom.setPrefRowCount(2);

        // Layout simple
        GridPane gp = new GridPane();
        gp.setHgap(8); gp.setVgap(8); gp.setPadding(new Insets(10));
        gp.addRow(0, new Label("Motivo consulta*:"), txtMotivo);
        gp.addRow(1, new Label("Alergias:"),         txtAlergias);
        gp.addRow(2, new Label("Medicamentos:"),     txtMedic);
        gp.addRow(3, new Label("Recomendaciones:"),  txtRecom);
        dlg.getDialogPane().setContent(gp);

        // Validación: habilitar Guardar solo si hay motivo
        Node ok = dlg.getDialogPane().lookupButton(BTN_GUARDAR);
        ok.setDisable(true);
        txtMotivo.textProperty().addListener((o, ov, nv) -> ok.setDisable(nv == null || nv.isBlank()));

        var res = dlg.showAndWait();
        if (res.isEmpty() || res.get() != BTN_GUARDAR) return;

        final String motivoConsulta   = txtMotivo.getText().trim();
        final String alergias         = emptyToNull(txtAlergias.getText());
        final String medicamentos     = emptyToNull(txtMedic.getText());
        final String recomendaciones  = emptyToNull(txtRecom.getText());

        try {
            // Ahora llamamos al service SIN 'texto'
            vistaActual = historiaService.agregarNotaPorCodigo(
                    codigo.trim(),
                    alergias,
                    medicamentos,
                    motivoConsulta,
                    recomendaciones
            );

            if (tblNotas != null) tblNotas.getItems().setAll(vistaActual.notas);
            setHistoriaIdNotasLabel(vistaActual.historiaId);
            info("Nota agregada", "Se agregó una nota a la historia #" + vistaActual.historiaId);

        } catch (Exception ex) {
            error("Error agregando nota", exceptionChain(ex));
        }
    }

    /** Convierte cadenas vacías a null para que el DAO inserte NULL en la BD. */
    private static String emptyToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /* ========= Helpers públicos para Dashboard ========= */

    /** Permite precargar la historia por código desde el Dashboard. */
    public void cargarPaciente(String codigoPaciente) {
        if (txtCodigoPaciente != null) {
            txtCodigoPaciente.setText(codigoPaciente);
        }
        abrirHistoriaPorCodigo(codigoPaciente);
    }

    /** Cambia a la pestaña "Notas". */
    public void abrirPestanaNotas() {
        if (tabPane != null) {
            tabPane.getSelectionModel().select(1);
        }
    }

    /** Cambia a la pestaña "Historia Clínica". */
    public void abrirPestanaHistoria() {
        if (tabPane != null) {
            tabPane.getSelectionModel().select(0);
        }
    }

    /* ========= Utilidades UI ========= */
    private void setHistoriaIdLabel(Object id) {
        if (lblHistoriaId != null) {
            lblHistoriaId.setText(id == null ? "—" : String.valueOf(id));
        }
    }
    private void setPacienteLabel(String nombre) {
        if (lblPaciente != null) {
            lblPaciente.setText(nombre == null || nombre.isBlank() ? "—" : nombre);
        }
    }
    private void setHistoriaIdNotasLabel(Object id) {
        if (lblHistoriaIdNotas != null) {
            lblHistoriaIdNotas.setText(id == null ? "—" : String.valueOf(id));
        }
    }

    private String formatTs(Timestamp ts) {
        if (ts == null) return "";
        return ts.toInstant().atZone(ZoneId.systemDefault()).format(FECHA_FMT);
    }
    private static String safe(String s) { return s == null ? "" : s; }

    private static void info(String header, String content) {
        showAlert(Alert.AlertType.INFORMATION, "Información", header, content);
    }
    private static void warn(String header, String content) {
        showAlert(Alert.AlertType.WARNING, "Aviso", header, content);
    }
    private static void error(String header, String content) {
        showAlert(Alert.AlertType.ERROR, "Error", header, content);
    }
    private static void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(header);
        a.setContentText(content);
        a.show();
    }

    private static String exceptionChain(Throwable ex) {
        StringBuilder sb = new StringBuilder();
        Throwable cur = ex;
        while (cur != null) {
            if (sb.length() > 0) sb.append("\ncausado por: ");
            sb.append(cur.getClass().getSimpleName())
              .append(": ")
              .append(Objects.toString(cur.getMessage(), ""));
            cur = cur.getCause();
        }
        return sb.toString();
    }
}
