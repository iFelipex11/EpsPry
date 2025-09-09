package pfx;

import bdpryfinal.CitaDaoJdbc;
import bdpryfinal.CitaService;
import bdpryfinal.DoctorDaoJdbc;
import bdpryfinal.AuthService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDate;

public class DashboardPacienteController {

  @FXML private Label lblBienvenida, lblMsg;
  @FXML private DatePicker dpFrom, dpTo, dpFecha;
  @FXML private TableView<CitaVM> tblCitas;
  @FXML private TableColumn<CitaVM, String> colFecha, colHora, colDoctor, colEstado, colObs;
  @FXML private ComboBox<DoctorDaoJdbc.DoctorItem> cmbDoctor;
  @FXML private TextField txtHora;
  @FXML private TextArea txtObs;

  private final CitaService citaSrv = new CitaService();
  private final DoctorDaoJdbc doctorDao = new DoctorDaoJdbc();
  private final AuthService auth = new AuthService();

  private final ObservableList<CitaVM> data = FXCollections.observableArrayList();

  @FXML
  private void initialize() {
    var u = SessionContext.currentUser;
    lblBienvenida.setText(u == null ? "Sin sesión" : "Bienvenido, " + u.nombre + " (Paciente)");
    dpFrom.setValue(LocalDate.now().minusMonths(1));
    dpTo.setValue(LocalDate.now().plusMonths(1));
    dpFecha.setValue(LocalDate.now());

    colFecha.setCellValueFactory(c -> c.getValue().fecha);
    colHora.setCellValueFactory(c -> c.getValue().hora);
    colDoctor.setCellValueFactory(c -> c.getValue().doctor);
    colEstado.setCellValueFactory(c -> c.getValue().estado);
    colObs.setCellValueFactory(c -> c.getValue().obs);
    tblCitas.setItems(data);

    // cargar doctores para "Nueva cita"
    var doctores = doctorDao.listarTodos();
    cmbDoctor.setItems(FXCollections.observableArrayList(doctores));
    cmbDoctor.setConverter(new StringConverterDoctor());

    refrescarCitas();
  }

  @FXML
  private void refrescarCitas() {
    lblMsg.setText("");
    var pid = SessionContext.pacienteId;
    if (pid == null) { lblMsg.setText("Sesión sin paciente enlazado."); return; }
    try {
      var items = citaSrv.citasDePaciente(pid, dpFrom.getValue(), dpTo.getValue());
      data.setAll(items.stream().map(CitaVM::from).toList());
      if (data.isEmpty()) lblMsg.setText("(sin citas en el rango)");
    } catch (Exception e) {
      lblMsg.setText("Error: " + e.getMessage());
    }
  }

  @FXML
  private void cancelarCita() {
    lblMsg.setText("");
    var sel = tblCitas.getSelectionModel().getSelectedItem();
    if (sel == null) { lblMsg.setText("Selecciona una cita."); return; }
    try {
      citaSrv.cancelarPorPaciente(Integer.parseInt(sel.id.get()), SessionContext.pacienteId);
      lblMsg.setStyle("-fx-text-fill: green;");
      lblMsg.setText("Cita cancelada.");
      refrescarCitas();
    } catch (Exception e) {
      lblMsg.setStyle("-fx-text-fill: red;");
      lblMsg.setText("No se pudo cancelar: " + e.getMessage());
    }
  }

  @FXML
  private void solicitarCita() {
    lblMsg.setText("");
    var pid = SessionContext.pacienteId;
    var doc = cmbDoctor.getSelectionModel().getSelectedItem();
    var fecha = dpFecha.getValue();
    String horaTxt = (txtHora.getText() == null) ? "" : txtHora.getText().trim();
    if (pid == null) { lblMsg.setText("Sesión sin paciente enlazado."); return; }
    if (doc == null || fecha == null || horaTxt.isEmpty()) { lblMsg.setText("Completa doctor, fecha y hora."); return; }

    java.time.LocalTime hora;
    try { hora = java.time.LocalTime.parse(horaTxt + ":00"); }
    catch (Exception ex) { lblMsg.setText("Hora inválida (usa HH:mm)"); return; }

    try {
      int id = citaSrv.crearPorPaciente(pid, doc.identificacion, fecha, hora, txtObs.getText());
      lblMsg.setStyle("-fx-text-fill: green;");
      lblMsg.setText("Solicitud enviada (id " + id + "). Queda Pendiente.");
      limpiarNueva();
      refrescarCitas();
    } catch (IllegalStateException e) {
      lblMsg.setStyle("-fx-text-fill: red;");
      lblMsg.setText("No creada: " + e.getMessage());
    } catch (Exception e) {
      lblMsg.setStyle("-fx-text-fill: red;");
      lblMsg.setText("Error: " + e.getMessage());
    }
  }

  @FXML
  private void limpiarNueva() {
    txtHora.clear();
    txtObs.clear();
    if (!cmbDoctor.getItems().isEmpty()) cmbDoctor.getSelectionModel().select(0);
    dpFecha.setValue(LocalDate.now());
  }


  @FXML
  private void logout() {
    try {
      if (SessionContext.currentSesionId != null) new AuthService().logout(SessionContext.currentSesionId);
      SessionContext.clear();
      Stage stage = (Stage) lblBienvenida.getScene().getWindow();
      Parent root = FXMLLoader.load(App.class.getResource("Start.fxml"));
      stage.setScene(new Scene(root));
      stage.setTitle("Clínica - Inicio");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /* ====== VM ====== */
  public static class CitaVM {
    final SimpleStringProperty id = new SimpleStringProperty();
    final SimpleStringProperty fecha = new SimpleStringProperty();
    final SimpleStringProperty hora = new SimpleStringProperty();
    final SimpleStringProperty doctor = new SimpleStringProperty();
    final SimpleStringProperty estado = new SimpleStringProperty();
    final SimpleStringProperty obs = new SimpleStringProperty();

    static CitaVM from(bdpryfinal.CitaDaoJdbc.CitaPacItem it) {
      var vm = new CitaVM();
      vm.id.set(String.valueOf(it.id));
      vm.fecha.set(it.fecha);
      vm.hora.set(it.hora);
      vm.doctor.set(it.doctor);
      vm.estado.set(it.estado);
      vm.obs.set(it.observacion == null ? "" : it.observacion);
      return vm;
    }
  }

  private static class StringConverterDoctor extends javafx.util.StringConverter<DoctorDaoJdbc.DoctorItem> {
    @Override public String toString(DoctorDaoJdbc.DoctorItem d) {
      if (d == null) return "";
      var esp = (d.especialidad == null || d.especialidad.isBlank()) ? "" : " (" + d.especialidad + ")";
      return d.nombre + esp + "  [" + d.identificacion + "]";
    }
    @Override public DoctorDaoJdbc.DoctorItem fromString(String s) { return null; }
  }
}
