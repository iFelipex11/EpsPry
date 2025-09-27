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

    // --- Campos comunes / navegación ---
    @FXML private ComboBox<String> cbRol;
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword, txtPassword2;

    // --- Campos específicos Paciente (coinciden con Register.fxml) ---
    @FXML private TextField txtCedula;
    @FXML private TextField txtNombre1;
    @FXML private TextField txtNombre2;
    @FXML private TextField txtApellido1;
    @FXML private TextField txtApellido2;
    @FXML private TextField txtCorreo;
    @FXML private TextField txtGenero;
    @FXML private TextField txtTelefono;
    @FXML private TextField txtDireccion;
    @FXML private DatePicker dpNacimiento;

    @FXML
    private void initialize() {
        if (cbRol != null) {
            cbRol.getItems().setAll("Paciente", "Doctor");
            cbRol.getSelectionModel().select("Paciente");
        }
    }

    @FXML
    private void onRoleChanged() {
        String rol = cbRol.getSelectionModel().getSelectedItem();
        if ("Doctor".equals(rol)) {
            switchScene((Node) cbRol, "RegisterDoctor.fxml");
        }
    }

    @FXML
    private void onCrearCuentaPaciente() {
        // --- Validaciones básicas ---
        String username = safe(txtUsername.getText());
        String pass1 = safe(txtPassword.getText());
        String pass2 = safe(txtPassword2.getText());

        String cedula = safe(txtCedula.getText());
        String nombre1 = safe(txtNombre1.getText());
        String nombre2 = safe(txtNombre2.getText());
        String apellido1 = safe(txtApellido1.getText());
        String apellido2 = safe(txtApellido2.getText());
        String correo = safe(txtCorreo.getText());
        String genero = safe(txtGenero.getText());
        String telefono = safe(txtTelefono.getText());
        String direccion = safe(txtDireccion.getText());
        LocalDate fnac = dpNacimiento.getValue();

        if (username.isEmpty() || pass1.isEmpty() || pass2.isEmpty()
                || cedula.isEmpty() || nombre1.isEmpty() || apellido1.isEmpty()
                || correo.isEmpty()) {
            warn("Completa los campos obligatorios: usuario, contraseñas, cédula, primer nombre, primer apellido y correo.");
            return;
        }
        if (!pass1.equals(pass2)) { warn("Las contraseñas no coinciden."); return; }
        if (!isEmail(correo)) { warn("Correo inválido."); return; }

        try {
            // 1) Crear usuario
            UsuarioDaoJdbc usuarioDao = new UsuarioDaoJdbc();
            int usuarioId = usuarioDao.crearUsuario(username, pass1, "Paciente");

            // 2) Insert/Upsert del perfil Paciente (firma nueva: SIN 'identificacion')
            PacienteDaoJdbc pacienteDao = new PacienteDaoJdbc();
            pacienteDao.insertarPerfilPaciente(
                    usuarioId,
                    cedula,
                    nombre1, nombre2, apellido1, apellido2,
                    correo, telefono, genero, direccion,
                    fnac
            );

            info("Cuenta de Paciente creada con éxito.");
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
