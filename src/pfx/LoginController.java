package pfx;

import bdpryfinal.AuthService;
import bdpryfinal.UsuarioDaoJdbc;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {

  @FXML private TextField txtUsuario;     // coincide con Login.fxml
  @FXML private PasswordField txtPass;    // coincide con Login.fxml
  @FXML private Label lblMsg;

  // Usa los servicios reales del proyecto
  private final AuthService auth = new AuthService();
  private final UsuarioDaoJdbc usuarioDao = new UsuarioDaoJdbc();

  @FXML
  private void entrar() {
    lblMsg.setText("");
    String username = txtUsuario.getText() == null ? "" : txtUsuario.getText().trim();
    String password = txtPass.getText() == null ? "" : txtPass.getText();

    if (username.isEmpty() || password.isEmpty()) {
      lblMsg.setText("Usuario y contraseña requeridos");
      return;
    }

    try {
      // Hace login y deja la sesión cargada en SessionContext
      var res = auth.login(username, password);

      // Cargar ventana según rol (res.user() es UsuarioDaoJdbc.User con campos públicos)
      String rol = res.user().rol;
      Stage stage = (Stage) txtUsuario.getScene().getWindow();
      Parent root;

      if ("Doctor".equalsIgnoreCase(rol)) {
        root = FXMLLoader.load(App.class.getResource("Dashboard.fxml"));
        stage.setTitle("Clínica - Panel Doctor");
      } else {
        root = FXMLLoader.load(App.class.getResource("DashboardPaciente.fxml"));
        stage.setTitle("Clínica - Mi Portal Paciente");
      }

      stage.setScene(new Scene(root));
      stage.show();

    } catch (IllegalArgumentException e) {
      lblMsg.setText("Credenciales inválidas");
      txtPass.clear();
      txtPass.requestFocus();
    } catch (Exception e) {
      e.printStackTrace();
      lblMsg.setText("Error: " + e.getMessage());
    }
  }

  @FXML
  private void recuperarContrasena() {
    lblMsg.setText("");
    String username = txtUsuario.getText() == null ? "" : txtUsuario.getText().trim();
    if (username.isEmpty()) {
      lblMsg.setText("Ingrese su usuario para recuperar contraseña");
      txtUsuario.requestFocus();
      return;
    }

    try {
      String pass = usuarioDao.obtenerPasswordPlano(username);
      if (pass == null) {
        lblMsg.setText("Usuario no encontrado");
        return;
      }
      Alert a = new Alert(Alert.AlertType.INFORMATION);
      a.setTitle("Recuperación de contraseña");
      a.setHeaderText("Usuario: " + username);
      a.setContentText("Su contraseña es: " + pass);
      a.showAndWait();
    } catch (Exception ex) {
      ex.printStackTrace();
      lblMsg.setText("Error al recuperar: " + ex.getMessage());
    }
  }
}
