package pfx;

import bdpryfinal.NotaDaoJdbc;
import bdpryfinal.NotaService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;

public class NotaController {

  @FXML TextField txtCodPaciente;
  @FXML private Label lblPaciente;
  @FXML private TableView<NotaVM> tblNotas;
  @FXML private TableColumn<NotaVM, String> colFecha, colTexto;
  @FXML private Button btnAgregar;
  @FXML private Label lblMsg;

  private final NotaService service = new NotaService();
  private final ObservableList<NotaVM> data = FXCollections.observableArrayList();
  private String codActual; // ahora guarda la CÉDULA

  @FXML
  private void initialize() {
    colFecha.setCellValueFactory(c -> c.getValue().fecha);
    colTexto.setCellValueFactory(c -> c.getValue().texto);
    tblNotas.setItems(data);
    btnAgregar.setDisable(true);
  }

  /** Método público para precargar el código desde otra ventana y abrir. */
  public void cargarPaciente(String codPaciente) {
    if (codPaciente == null || codPaciente.isBlank()) return;
    txtCodPaciente.setText(codPaciente);
    abrir();
  }

  /** Abre (o crea) la nota del paciente cuya CÉDULA está en el TextField. */
  @FXML
  public void abrir() {
    lblMsg.setText("");
    var cod = txtCodPaciente.getText() == null ? "" : txtCodPaciente.getText().trim();
    if (cod.isEmpty()) { lblMsg.setText("Ingrese la cédula del paciente."); return; }
    try {
      var v = service.abrirPorCodigoPaciente(cod); // busca por cédula
      codActual = v.paciente.cedula;               // <-- cedula
      lblPaciente.setText(v.paciente.nombre + " [" + v.paciente.cedula + "]"); // <-- cedula
      data.setAll(v.notas.stream().map(NotaVM::from).toList());
      btnAgregar.setDisable(false);
      if (data.isEmpty()) lblMsg.setText("(sin notas)");
    } catch (Exception e) {
      btnAgregar.setDisable(true);
      data.clear();
      lblPaciente.setText("—");
      lblMsg.setText("Error: " + e.getMessage());
    }
  }

  /** Agrega una entrada de texto a la nota actual. */
  @FXML
  private void agregarNota() {
    if (codActual == null) return;
    var dlg = new TextInputDialog();
    dlg.setTitle("Nueva nota");
    dlg.setHeaderText(null);
    dlg.setContentText("Texto de la nota:");
    var txt = dlg.showAndWait();
    if (txt.isEmpty()) return;
    try {
      var v = service.agregarNotaPorCodigo(codActual, txt.get().trim()); // usa cédula
      data.setAll(v.notas.stream().map(NotaVM::from).toList());
      lblMsg.setStyle("-fx-text-fill: green;");
      lblMsg.setText("Nota agregada.");
    } catch (Exception e) {
      lblMsg.setStyle("-fx-text-fill: red;");
      lblMsg.setText("Error: " + e.getMessage());
    }
  }

  @FXML
  private void cerrar() {
    Stage st = (Stage) lblPaciente.getScene().getWindow();
    st.close();
  }

  /* ==== VM ==== */
  public static class NotaVM {
    final SimpleStringProperty fecha = new SimpleStringProperty();
    final SimpleStringProperty texto = new SimpleStringProperty();
    static NotaVM from(NotaDaoJdbc.NotaTxt n) {
      var vm = new NotaVM();
      var fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
      vm.fecha.set(n.creadaEn.toLocalDateTime().format(fmt));
      vm.texto.set(n.texto == null ? "" : n.texto);
      return vm;
    }
  }
}
