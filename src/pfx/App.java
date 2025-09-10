package pfx;

import bdpryfinal.Db;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class App extends Application {
  @Override
  public void start(Stage stage) throws Exception {
    // Cargar el archivo FXML
    Parent root = FXMLLoader.load(getClass().getResource("Start.fxml"));
    
    // Establecer el título de la ventana
    stage.setTitle("Clininova");

    // Establecer el ícono de la ventana
    Image icon = new Image("/Imgs/Logo.jpeg"); // Reemplaza con la ruta correcta a tu imagen
    stage.getIcons().add(icon);

    // Configurar la escena y mostrar la ventana
    stage.setScene(new Scene(root));
    stage.show();
  }

  @Override
  public void stop() throws Exception {
    Db.closeQuiet();
    super.stop();
  }

  public static void main(String[] args) {
    launch(args);
  }
}
