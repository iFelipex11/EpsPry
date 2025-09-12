package pfx;

import bdpryfinal.PacienteDaoJdbc;
import bdpryfinal.UsuarioDaoJdbc;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDate;

public class RegisterController {

    @FXML private ComboBox<String> cbRol;
    @FXML private TextField txtUsername, txtNombre, txtGenero, txtTelefono, txtDireccion;
    @FXML private PasswordField txtPassword, txtPassword2;
    @FXML private DatePicker dpNacimiento;

    @FXML
    private void initialize() {
        cbRol.getItems().setAll("Paciente", "Doctor");
        cbRol.getSelectionModel().select("Paciente"); // por defecto Paciente
    }

    @FXML
    private void onRoleChanged() {
        String rol = cbRol.getSelectionModel().getSelectedItem();
        if ("Doctor".equals(rol)) {
            switchScene((Node) cbRol, "RegisterDoctor.fxml"); // <- ruta corregida
        }
    }

    @FXML
    private void onCrearCuentaPaciente() {
        String username = safe(txtUsername.getText());
        String nombre   = safe(txtNombre.getText());
        String pass1    = txtPassword.getText();
        String pass2    = txtPassword2.getText();

        if (username.isEmpty() || nombre.isEmpty() || pass1.isEmpty() || pass2.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Completa los campos obligatorios.");
            return;
        }
        if (!pass1.equals(pass2)) {
            showAlert(Alert.AlertType.WARNING, "Las contraseñas no coinciden.");
            return;
        }

        LocalDate fnac = dpNacimiento.getValue();
        String genero  = safe(txtGenero.getText());
        String tel     = safe(txtTelefono.getText());
        String dir     = safe(txtDireccion.getText());

        try {
            // 1) Crear usuario PACIENTE
            UsuarioDaoJdbc usuarioDao = new UsuarioDaoJdbc();
            int usuarioId = usuarioDao.crearUsuario(username, nombre, pass1, "Paciente");

            // 2) Completar perfil paciente
            PacienteDaoJdbc pacienteDao = new PacienteDaoJdbc();
            pacienteDao.completarPerfilPorUsuarioId(usuarioId, fnac, genero, tel, dir);

            showAlert(Alert.AlertType.INFORMATION, "Cuenta de Paciente creada con éxito.");
            // Puedes redirigir a Login o Start:
            // switchScene((Node) cbRol, "Login.fxml");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "No se pudo crear la cuenta: " + e.getMessage());
        }
    }

    @FXML
    private void onVolver() {
            Stage stage = (Stage) cbRol.getScene().getWindow();
            stage.close();
    }

    /* ===== Helpers ===== */

    private String safe(String s) { return s == null ? "" : s.trim(); }

    private void switchScene(Node anyNode, String fxmlName) {
        try {
            Stage stage = (Stage) anyNode.getScene().getWindow();
            Scene scene = new Scene(FXMLLoader.load(App.class.getResource(fxmlName)));
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Error cargando vista: " + fxmlName + "\n" + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String msg) {
        Alert a = new Alert(type, msg, ButtonType.OK);
        a.showAndWait();
    }
}
