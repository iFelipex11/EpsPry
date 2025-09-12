package pfx;

import bdpryfinal.DoctorDaoJdbc;
import bdpryfinal.UsuarioDaoJdbc;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;

public class RegisterDoctorController {

    @FXML private ComboBox<String> cbRol;
    @FXML private TextField txtUsername, txtNombre, txtEspecialidad, txtSede, txtHorario;
    @FXML private PasswordField txtPassword, txtPassword2;

    @FXML
    private void initialize() {
        cbRol.getItems().setAll("Paciente", "Doctor");
        cbRol.getSelectionModel().select("Doctor"); // seleccionado por defecto en esta vista
    }

    @FXML
    private void onRoleChanged() {
        String rol = cbRol.getSelectionModel().getSelectedItem();
        if ("Paciente".equals(rol)) {
            switchScene((Node) cbRol, "Register.fxml"); // <- ruta corregida
        }
    }

    @FXML
    private void onCrearCuentaDoctor() {
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

        String esp  = safe(txtEspecialidad.getText());
        String sede = safe(txtSede.getText());
        String hor  = safe(txtHorario.getText());

        try {
            // 1) Crear usuario DOCTOR
            UsuarioDaoJdbc usuarioDao = new UsuarioDaoJdbc();
            int usuarioId = usuarioDao.crearUsuario(username, nombre, pass1, "Doctor");

            // 2) Completar perfil Doctor
            DoctorDaoJdbc doctorDao = new DoctorDaoJdbc();
            doctorDao.completarPerfilPorUsuarioId(usuarioId, esp, sede, hor);

            showAlert(Alert.AlertType.INFORMATION, "Cuenta de Doctor creada con éxito.");
            // switchScene((Node) cbRol, "Login.fxml");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "No se pudo crear la cuenta: " + e.getMessage());
        }
    }

    @FXML
    private void onVolver() {
        switchScene((Node) cbRol, "Start.fxml"); // <- ruta corregida
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
