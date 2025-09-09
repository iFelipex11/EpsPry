package pfx;

import bdpryfinal.UsuarioDaoJdbc;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class RegisterController {

  @FXML private TextField txtUsuario;
  @FXML private TextField txtNombre;
  @FXML private PasswordField txtPass;
  @FXML private ComboBox<String> cmbRol;
  @FXML private Label lblMsg;

  private final UsuarioDaoJdbc usuarioDao = new UsuarioDaoJdbc();

  @FXML
  private void initialize() {
    cmbRol.getItems().addAll("Doctor", "Paciente");
    cmbRol.getSelectionModel().select("Paciente");
  }

  @FXML
  private void crear() {
    lblMsg.setText("");
    String u = val(txtUsuario.getText());
    String n = val(txtNombre.getText());
    String p = txtPass.getText() == null ? "" : txtPass.getText();
    String r = cmbRol.getValue();

    if (u.isEmpty() || n.isEmpty() || p.isEmpty() || r == null) {
      lblMsg.setText("Complete todos los campos.");
      return;
    }
    try {
      usuarioDao.crearUsuario(u, n, p, r);
      new Alert(Alert.AlertType.INFORMATION, "Usuario creado con éxito.").showAndWait();
      cerrar();
    } catch (IllegalStateException ex) {
      lblMsg.setText(ex.getMessage());
    } catch (Exception e) {
      lblMsg.setText("Error: " + e.getMessage());
    }
  }

  @FXML
  private void cerrar() {
    Stage st = (Stage) cmbRol.getScene().getWindow();
    st.close();
  }

  private static String val(String s) { return s == null ? "" : s.trim(); }
}
