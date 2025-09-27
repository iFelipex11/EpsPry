package bdpryfinal;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Db {
  //Conexion estatica que va a usar toda la app desde el metodo get()
  private static Connection shared;

  // Verificamos uqe la conexion exista, en caso de que no exista o este cerrada crea una nueva y la asigna a shared
  // SQLException nos ayuda a manejar las excepciones que puedan surgir
  public static Connection get() throws SQLException {
    if (shared == null || shared.isClosed()) {
      String url  = "jdbc:mysql://127.0.0.1:3306/clinica?useSSL=false&serverTimezone=UTC";
      String user = "root";
      String pass = "";
      shared = DriverManager.getConnection(url, user, pass);
    }
    return shared;
  }

  // Cuando queremos cerrar sesion invocamos el metodo vacio CloseQuiet con un try
  public static void closeQuiet() {
    if (shared != null) {
      try { shared.close();
      } 
      catch (Exception ignored) {}
      shared = null;
    }
  }
}
