package pfx;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class StartController {

  @FXML
  private void abrirLogin() {
    try {
      // Obtengo la ventana actual (la que muestra Start.fxml)
      Stage win = (Stage) javafx.stage.Window.getWindows()
          .filtered(w -> w.isShowing()).get(0);

      // Cargo el Login
      Parent root = FXMLLoader.load(App.class.getResource("Login.fxml"));
      win.setScene(new Scene(root));
      win.setTitle("Clínica - Login");
    } catch (Exception e) {
      throw new RuntimeException("No se pudo abrir Login: " + e.getMessage(), e);
    }
  }

  @FXML
  private void abrirRegistro() {
    try {
      FXMLLoader loader = new FXMLLoader(App.class.getResource("Register.fxml"));
      Parent root = loader.load();

      Stage st = new Stage();
      st.initOwner(javafx.stage.Window.getWindows().filtered(w -> w.isShowing()).get(0));
      st.initModality(Modality.WINDOW_MODAL);
      st.setTitle("Crear cuenta");
      st.setScene(new Scene(root));
      st.show();
    } catch (Exception e) {
      throw new RuntimeException("No se pudo abrir Registro: " + e.getMessage(), e);
    }
  }
}
