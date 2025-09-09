package pfx;

import bdpryfinal.HistoriaDaoJdbc;
import bdpryfinal.HistoriaService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;

public class HistoriaController {

  @FXML TextField txtCodPaciente; // package-private para poder setearlo desde Dashboard si quisieras
  @FXML private Label lblPaciente;
  @FXML private TableView<NotaVM> tblNotas;
  @FXML private TableColumn<NotaVM, String> colFecha, colTexto;
  @FXML private Button btnAgregar;
  @FXML private Label lblMsg;

  private final HistoriaService service = new HistoriaService();
  private final ObservableList<NotaVM> data = FXCollections.observableArrayList();
  private String codActual;

  @FXML
  private void initialize() {
    colFecha.setCellValueFactory(c -> c.getValue().fecha);
    colTexto.setCellValueFactory(c -> c.getValue().texto);
    tblNotas.setItems(data);
    btnAgregar.setDisable(true);
  }

  /** Método público para precargar código desde otra ventana y abrir la historia. */
  public void cargarPaciente(String codPaciente) {
    if (codPaciente == null || codPaciente.isBlank()) return;
    txtCodPaciente.setText(codPaciente);
    abrirHistoria();
  }

  /** Abre (o crea) la historia del paciente cuyo código está en el TextField. */
  @FXML
  public void abrirHistoria() {
    lblMsg.setText("");
    var cod = txtCodPaciente.getText() == null ? "" : txtCodPaciente.getText().trim();
    if (cod.isEmpty()) { lblMsg.setText("Ingrese el código del paciente."); return; }
    try {
      var v = service.abrirPorCodigoPaciente(cod);
      codActual = v.paciente.identificacion;
      lblPaciente.setText(v.paciente.nombre + " [" + v.paciente.identificacion + "]");
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

  /** Pide texto y agrega nota a la historia actual. */
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
      var v = service.agregarNotaPorCodigo(codActual, txt.get().trim());
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

    static NotaVM from(HistoriaDaoJdbc.Nota n) {
      var vm = new NotaVM();
      var fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
      vm.fecha.set(n.creadaEn.toLocalDateTime().format(fmt));
      vm.texto.set(n.texto);
      return vm;
    }
  }
}
