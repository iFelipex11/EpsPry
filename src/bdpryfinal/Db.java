package bdpryfinal;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Db {
  private static Connection shared;

  /** Devuelve conexión compartida (la crea si no existe). */
  public static Connection get() throws SQLException {
    if (shared == null || shared.isClosed()) {
      String url  = System.getProperty("DB_URL", "jdbc:mysql://127.0.0.1:3306/clinica?useSSL=false&serverTimezone=UTC");
      String user = System.getProperty("DB_USER", "root");
      String pass = System.getProperty("DB_PASS", "");
      shared = DriverManager.getConnection(url, user, pass);
    }
    return shared;
  }

  /** Cierra la conexión si está abierta, sin lanzar excepción. */
  public static void closeQuiet() {
    if (shared != null) {
      try { shared.close(); } catch (Exception ignored) {}
      shared = null;
    }
  }
}
