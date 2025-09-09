package pfx;

import bdpryfinal.Db;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {
  @Override
  public void start(Stage stage) throws Exception {
    Parent root = FXMLLoader.load(getClass().getResource("Start.fxml"));
    stage.setTitle("Clínica - Inicio");
    stage.setScene(new Scene(root));
    stage.show();
  }

  @Override
  public void stop() throws Exception {
    Db.closeQuiet();
    super.stop();
  }

  public static void main(String[] args) { launch(args); }
}
