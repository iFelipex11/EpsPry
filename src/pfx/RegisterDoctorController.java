package pfx;

import bdpryfinal.UsuarioDaoJdbc;
import bdpryfinal.DoctorDaoJdbc;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;

public class RegisterDoctorController {

    // --- Navegación / comunes ---
    @FXML private ComboBox<String> cbRol;
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword, txtPassword2;

    // --- Específicos Doctor (coinciden con RegisterDoctor.fxml) ---
    @FXML private TextField txtNombre1;
    @FXML private TextField txtNombre2;
    @FXML private TextField txtApellido1;
    @FXML private TextField txtApellido2;

    @FXML private TextField txtCorreo;
    @FXML private TextField txtTelefono;
    @FXML private TextField txtGenero;

    @FXML private TextField txtEspecialidad;
    @FXML private TextField txtSede;
    @FXML private TextField txtHorario;

    @FXML
    private void initialize() {
        if (cbRol != null) {
            cbRol.getItems().setAll("Paciente", "Doctor");
            cbRol.getSelectionModel().select("Doctor");
        }
    }

    @FXML
    private void onRoleChanged() {
        String rol = cbRol.getSelectionModel().getSelectedItem();
        if ("Paciente".equals(rol)) {
            switchScene((Node) cbRol, "Register.fxml");
        }
    }

    @FXML
    private void onCrearCuentaDoctor() {
        // --- Lectura y validaciones básicas ---
        String username = safe(txtUsername.getText());
        String pass1 = safe(txtPassword.getText());
        String pass2 = safe(txtPassword2.getText());

        String nombre1 = safe(txtNombre1.getText());
        String nombre2 = safe(txtNombre2.getText());
        String apellido1 = safe(txtApellido1.getText());
        String apellido2 = safe(txtApellido2.getText());

        String correo = safe(txtCorreo.getText());
        String telefono = safe(txtTelefono.getText());
        String genero = safe(txtGenero.getText());

        String especialidad = safe(txtEspecialidad.getText());
        String sede = safe(txtSede.getText());
        String horario = safe(txtHorario.getText());

        if (username.isEmpty() || pass1.isEmpty() || pass2.isEmpty()
                || nombre1.isEmpty() || apellido1.isEmpty()
                || correo.isEmpty() || especialidad.isEmpty()) {
            warn("Completa los campos obligatorios: usuario, contraseñas, primer nombre, primer apellido, correo y especialidad.");
            return;
        }
        if (!pass1.equals(pass2)) { warn("Las contraseñas no coinciden."); return; }
        if (!isEmail(correo)) { warn("Correo inválido."); return; }

        try {
            // 1) Crear usuario con rol DOCTOR (sin 'nombre' en tabla)
            UsuarioDaoJdbc usuarioDao = new UsuarioDaoJdbc();
            int usuarioId = usuarioDao.crearUsuario(username, pass1, "Doctor");

            // 2) Upsert del perfil de Doctor (actualiza nombres y extras)
            DoctorDaoJdbc doctorDao = new DoctorDaoJdbc();
            doctorDao.insertarPerfilDoctor(
                usuarioId,
                "DOC-" + usuarioId,  // identificacion (mantenemos el patrón del trigger)
                "CED-" + usuarioId,  // cedula     (igual que el trigger)
                nombre1, nombre2, apellido1, apellido2,
                especialidad, sede, horario,
                correo, telefono, genero
            );

            info("Cuenta de Doctor creada con éxito.");
            // switchScene((Node) cbRol, "Login.fxml");
        } catch (Exception e) {
            error("No se pudo crear la cuenta: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onVolver() {
        Stage stage = (Stage) cbRol.getScene().getWindow();
        stage.close();
    }

    /* ================= Helpers ================ */

    private String safe(String s) { return s == null ? "" : s.trim(); }
    private boolean isEmail(String s) { return s != null && s.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"); }

    private void switchScene(Node anyNode, String fxmlName) {
        try {
            Stage stage = (Stage) anyNode.getScene().getWindow();
            Scene scene = new Scene(FXMLLoader.load(App.class.getResource(fxmlName)));
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            error("Error cargando vista: " + fxmlName + "\n" + e.getMessage());
        }
    }

    private void warn(String msg) { showAlert(Alert.AlertType.WARNING, msg); }
    private void info(String msg) { showAlert(Alert.AlertType.INFORMATION, msg); }
    private void error(String msg) { showAlert(Alert.AlertType.ERROR, msg); }

    private void showAlert(Alert.AlertType type, String msg) {
        Alert a = new Alert(type, msg, ButtonType.OK);
        a.showAndWait();
    }
}
