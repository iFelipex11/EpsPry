package pfx;

import bdpryfinal.AuthService;
import bdpryfinal.CitaDaoJdbc;
import bdpryfinal.CitaService;
import bdpryfinal.DoctorDaoJdbc;
import bdpryfinal.PacienteDaoJdbc;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;

import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import javafx.scene.layout.VBox;
import javafx.geometry.Insets;

import javafx.stage.Modality;
import javafx.stage.Stage;

import javafx.util.StringConverter;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

  @FXML private Label lblBienvenida;
  @FXML private ComboBox<DoctorDaoJdbc.DoctorItem> cmbDoctor;
  @FXML private DatePicker dpFecha;
  @FXML private TableView<AgendaVM> tblAgenda;
  @FXML private TableColumn<AgendaVM, String> colHora, colPaciente, colEstado, colObs;
  @FXML private Label lblMsg;

  private final AuthService auth = new AuthService();
  private final CitaService citaSrv = new CitaService();
  private final DoctorDaoJdbc doctorDao = new DoctorDaoJdbc();
  private final PacienteDaoJdbc pacDao = new PacienteDaoJdbc();

  private final ObservableList<AgendaVM> data = FXCollections.observableArrayList();

  @Override
  public void initialize(URL url, ResourceBundle rb) {
    var u = SessionContext.currentUser;
    lblBienvenida.setText(u == null ? "Sin sesión"
        : "Bienvenido, " + u.nombre + " (" + u.rol + ")");

    dpFecha.setValue(LocalDate.now());

    colHora.setCellValueFactory(c -> c.getValue().hora);
    colPaciente.setCellValueFactory(c -> c.getValue().paciente);
    colEstado.setCellValueFactory(c -> c.getValue().estado);
    colObs.setCellValueFactory(c -> c.getValue().observacion);
    tblAgenda.setItems(data);

    cmbDoctor.setConverter(new StringConverterDoctor());

    if (esDoctorEnSesion()) {
      // Cargar todos y filtrar SOLO el doctor de la sesión
      var todos = doctorDao.listarTodos();
      var mio = todos.stream()
          .filter(d -> d.id == SessionContext.doctorId)
          .findFirst()
          .orElse(null);

      if (mio != null) {
        cmbDoctor.setItems(FXCollections.observableArrayList(mio));
        cmbDoctor.getSelectionModel().select(0);
        cmbDoctor.setDisable(true);               // 👈 Bloqueado para el doctor
      } else {
        // Fallback: dejar vacío y notificar
        cmbDoctor.setItems(FXCollections.observableArrayList());
        cmbDoctor.setDisable(true);
        lblMsg.setText("No se encontró tu perfil de Doctor.");
      }
    } else {
      // Rol Paciente (o futuro admin): se listan todos y queda habilitado
      var doctores = doctorDao.listarTodos();
      cmbDoctor.setItems(FXCollections.observableArrayList(doctores));
      if (!doctores.isEmpty()) cmbDoctor.getSelectionModel().select(0);
      cmbDoctor.setDisable(false);
    }
  }

  /** Muestra un diálogo con buscador para seleccionar un paciente. */
  private PacienteDaoJdbc.PacienteItem seleccionarPaciente() {
    Dialog<PacienteDaoJdbc.PacienteItem> dlg = new Dialog<>();
    dlg.setTitle("Seleccionar paciente");
    dlg.setHeaderText("Busca por nombre y selecciona un paciente");

    ButtonType btnOk = new ButtonType("Seleccionar", ButtonBar.ButtonData.OK_DONE);
    dlg.getDialogPane().getButtonTypes().addAll(btnOk, ButtonType.CANCEL);

    // Contenido: buscador + lista
    TextField txtBuscar = new TextField();
    txtBuscar.setPromptText("Escribe parte del nombre…");

    ListView<PacienteDaoJdbc.PacienteItem> list = new ListView<>();
    list.setPrefHeight(260);

    // Carga inicial (todos)
    var items = FXCollections.observableArrayList(pacDao.buscarPorNombre(""));
    list.setItems(items);

    // Filtrado en vivo consultando a la BD
    txtBuscar.textProperty().addListener((obs, ov, nv) -> {
      try {
        items.setAll(pacDao.buscarPorNombre(nv));
      } catch (Exception ex) {
        // si algo pasa, dejamos la lista como está
      }
    });

    VBox box = new VBox(8, txtBuscar, list);
    box.setPadding(new Insets(10));
    dlg.getDialogPane().setContent(box);

    // Selección por doble clic
    list.setOnMouseClicked(ev -> {
      if (ev.getClickCount() == 2 && list.getSelectionModel().getSelectedItem() != null) {
        dlg.setResult(list.getSelectionModel().getSelectedItem());
        dlg.close();
      }
    });

    // Habilitar OK solo si hay selección
    Node okNode = dlg.getDialogPane().lookupButton(btnOk);
    okNode.setDisable(true);
    list.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> okNode.setDisable(b == null));

    dlg.setResultConverter(bt -> bt == btnOk ? list.getSelectionModel().getSelectedItem() : null);

    return dlg.showAndWait().orElse(null);
  }

  @FXML
  private void cargarAgenda() {
    lblMsg.setText("");
    var doc = cmbDoctor.getSelectionModel().getSelectedItem();
    var fecha = dpFecha.getValue();
    if (doc == null || fecha == null) {
      lblMsg.setText("Selecciona doctor y fecha.");
      return;
    }
    try {
      var items = citaSrv.agendaDeDoctor(doc.cedula, fecha); // <-- usar cédula
      data.setAll(items.stream().map(AgendaVM::from).toList());
      if (data.isEmpty()) lblMsg.setText("(sin citas)");
    } catch (Exception e) {
      lblMsg.setText("Error: " + e.getMessage());
      e.printStackTrace();
    }
  }

  // ====================== Abrir Historia ======================
  @FXML
  private void abrirHistoria() {
    lblMsg.setText("");
    var sel = seleccionarPaciente();
    if (sel == null) return; // cancelado
    abrirVentanaHistoria(sel.cedula, /*abrirNotas=*/false); // <-- usar cédula
  }

  // ====================== Abrir Notas ======================
  @FXML
  private void abrirNotas() {
    lblMsg.setText("");
    var sel = seleccionarPaciente();
    if (sel == null) return; // cancelado

    try {
      FXMLLoader loader = new FXMLLoader(App.class.getResource("Nota.fxml"));
      Parent root = loader.load();

      // pasar el código del paciente al controller
      pfx.NotaController ctrl = loader.getController();
      ctrl.cargarPaciente(sel.cedula); // <-- usar cédula

      Stage st = new Stage();
      st.setTitle("Notas — " + sel.nombre + " [" + sel.cedula + "]"); // <-- usar cédula
      st.setScene(new Scene(root));
      st.initOwner(lblMsg.getScene().getWindow());
      st.initModality(javafx.stage.Modality.WINDOW_MODAL);
      st.show();

    } catch (java.io.IOException ex) {
      lblMsg.setStyle("-fx-text-fill:red;");
      lblMsg.setText("No se pudo abrir Notas: " + ex.getMessage());
      ex.printStackTrace();
    }
  }

  /**
   * Abre Historia.fxml, carga el paciente por código (cédula) y selecciona la pestaña deseada.
   */
  private void abrirVentanaHistoria(String codPaciente, boolean abrirNotas) {
    try {
      FXMLLoader loader = new FXMLLoader(App.class.getResource("Historia.fxml"));
      Parent root = loader.load();

      HistoriaController ctrl = loader.getController();
      ctrl.cargarPaciente(codPaciente); // <-- pasa cédula

      if (abrirNotas) ctrl.abrirPestanaNotas();
      else ctrl.abrirPestanaHistoria();

      Stage st = new Stage();
      st.initOwner(lblBienvenida.getScene().getWindow());
      st.initModality(Modality.WINDOW_MODAL);
      st.setTitle(abrirNotas ? "Notas de historia" : "Historia clínica");
      st.setScene(new Scene(root));
      st.show();

    } catch (Exception ex) {
      lblMsg.setText("No se pudo abrir la historia: " + ex.getMessage());
      ex.printStackTrace();
    }
  }

  @FXML
  private void logout() {
    try {
      if (SessionContext.currentSesionId != null) auth.logout(SessionContext.currentSesionId);
      SessionContext.currentUser = null;
      SessionContext.currentSesionId = null;

      Stage stage = (Stage) lblBienvenida.getScene().getWindow();
      Parent root = FXMLLoader.load(App.class.getResource("Login.fxml"));
      stage.setScene(new Scene(root));
      stage.setTitle("Clínica - Login");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @FXML
  private void nuevaCita() {
    lblMsg.setText("");
    var doc = cmbDoctor.getSelectionModel().getSelectedItem();
    if (doc == null) { lblMsg.setText("Selecciona doctor."); return; }

    // ========== pedir fecha con DatePicker ==========
    java.time.LocalDate base =
        (dpFecha != null && dpFecha.getValue() != null) ? dpFecha.getValue() : java.time.LocalDate.now();

    Dialog<ButtonType> dlg = new Dialog<>();
    dlg.setTitle("Elegir fecha");
    dlg.setHeaderText("Selecciona el día");
    ButtonType OK = new ButtonType("Aceptar", ButtonBar.ButtonData.OK_DONE);
    dlg.getDialogPane().getButtonTypes().addAll(OK, ButtonType.CANCEL);

    DatePicker dp = new DatePicker(base);
    dp.setMaxWidth(Double.MAX_VALUE);
    dlg.getDialogPane().setContent(dp);

    var res = dlg.showAndWait();
    if (res.isEmpty() || res.get() != OK || dp.getValue() == null) return;

    var fecha = dp.getValue();
    if (dpFecha != null) dpFecha.setValue(fecha);

    // 1) Seleccionar paciente de la lista
    var pacSel = seleccionarPaciente();
    if (pacSel == null) return; // cancelado

    // 2) Pedir hora
    var dlgHora = new javafx.scene.control.TextInputDialog("10:00");
    dlgHora.setTitle("Nueva cita");
    dlgHora.setHeaderText(pacSel.nombre + " [" + pacSel.cedula + "]"); // <-- usar cédula
    dlgHora.setContentText("Hora (HH:mm):");
    var horaOpt = dlgHora.showAndWait();
    if (horaOpt.isEmpty()) return;

    java.time.LocalTime hora;
    try {
      var horaStr = horaOpt.get();
      if (horaStr.length() == 5) horaStr = horaStr + ":00";
      hora = java.time.LocalTime.parse(horaStr);
    } catch (Exception e) {
      lblMsg.setText("Hora inválida.");
      return;
    }

    // 3) Elegir estado
    var estados = java.util.List.of("Pendiente","Confirmada","Cancelada","Atendida");
    var ch = new ChoiceDialog<>(estados.get(1), estados);
    ch.setTitle("Nueva cita");
    ch.setHeaderText(pacSel.nombre + " [" + pacSel.cedula + "]"); // <-- usar cédula
    ch.setContentText("Estado:");
    var estadoOpt = ch.showAndWait();
    if (estadoOpt.isEmpty()) return;
    var estado = estadoOpt.get();

    // 4) Observación (opcional)
    var dlgObs = new javafx.scene.control.TextInputDialog();
    dlgObs.setTitle("Nueva cita");
    dlgObs.setHeaderText(pacSel.nombre + " [" + pacSel.cedula + "]"); // <-- usar cédula
    dlgObs.setContentText("Observación (opcional):");
    var obs = dlgObs.showAndWait().orElse("");

    // 5) Crear cita usando CÉDULAS de paciente y doctor
    try {
      int id = citaSrv.crearPorCodigos(pacSel.cedula, doc.cedula, fecha, hora, estado, obs); // <-- usar cédula
      lblMsg.setStyle("-fx-text-fill: green;");
      lblMsg.setText("Cita creada id=" + id);
      cargarAgenda();
    } catch (IllegalStateException e) {
      lblMsg.setStyle("-fx-text-fill: red;");
      lblMsg.setText("No creada: " + e.getMessage());
    } catch (Exception e) {
      lblMsg.setStyle("-fx-text-fill: red;");
      lblMsg.setText("Error creando cita: " + e.getMessage());
      e.printStackTrace();
    }
  }

  @FXML
  private void cambiarEstado() {
    lblMsg.setText("");
    var sel = tblAgenda.getSelectionModel().getSelectedItem();
    if (sel == null) { lblMsg.setText("Selecciona una fila en la tabla."); return; }

    var estados = java.util.List.of("Pendiente","Confirmada","Cancelada","Atendida");
    var ch = new ChoiceDialog<>(sel.estado.get(), estados);
    ch.setTitle("Cambiar estado");
    ch.setHeaderText(null);
    ch.setContentText("Nuevo estado:");
    var nuevoOpt = ch.showAndWait();
    if (nuevoOpt.isEmpty()) return;
    var nuevo = nuevoOpt.get();

    try {
      int citaId = Integer.parseInt(sel.id.get());
      citaSrv.actualizarEstado(citaId, nuevo);
      lblMsg.setStyle("-fx-text-fill: green;");
      lblMsg.setText("Cita " + citaId + " → " + nuevo);
      cargarAgenda();
    } catch (Exception e) {
      lblMsg.setStyle("-fx-text-fill: red;");
      lblMsg.setText("Error: " + e.getMessage());
      e.printStackTrace();
    }
  }

  /* ====== VM ====== */
  public static class AgendaVM {
    final SimpleStringProperty id          = new SimpleStringProperty();
    final SimpleStringProperty hora        = new SimpleStringProperty();
    final SimpleStringProperty paciente    = new SimpleStringProperty();
    final SimpleStringProperty estado      = new SimpleStringProperty();
    final SimpleStringProperty observacion = new SimpleStringProperty();

    static AgendaVM from(CitaDaoJdbc.AgendaItem it) {
      var vm = new AgendaVM();
      vm.id.set(String.valueOf(it.id));
      vm.hora.set(it.hora);
      vm.paciente.set(it.paciente);
      vm.estado.set(it.estado);
      vm.observacion.set(it.observacion == null ? "" : it.observacion);
      return vm;
    }
  }

  private static class StringConverterDoctor extends StringConverter<DoctorDaoJdbc.DoctorItem> {
    @Override public String toString(DoctorDaoJdbc.DoctorItem d) {
      if (d == null) return "";
      var esp = (d.especialidad == null || d.especialidad.isBlank()) ? "" : " (" + d.especialidad + ")";
      return d.nombre + esp + "  [" + d.cedula + "]"; // <-- usar cédula
    }
    @Override public DoctorDaoJdbc.DoctorItem fromString(String s) { return null; }
  }

  private boolean esDoctorEnSesion() {
    return SessionContext.currentUser != null
        && "Doctor".equalsIgnoreCase(SessionContext.currentUser.rol)
        && SessionContext.doctorId != null;
  }
}
