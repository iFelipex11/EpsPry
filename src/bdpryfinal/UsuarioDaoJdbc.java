package bdpryfinal;

import java.sql.*;

/**
 * DAO para Usuario y Sesion, y resolución de vínculos a Paciente/Doctor.
 */
public class UsuarioDaoJdbc {

  /** DTO de usuario para usar en la app */
  public static final class User {
    public final int id;
    public final String username;
    public final String nombre;
    public final String rol;

    public User(int id, String username, String nombre, String rol) {
      this.id = id;
      this.username = username;
      this.nombre = nombre;
      this.rol = rol;
    }
  }

  /** Crea usuario (para tu registro) */
  public int crearUsuario(String username, String nombre, String passwordPlano, String rol) {
    String check = "SELECT 1 FROM Usuario WHERE username = ?";
    String ins   = "INSERT INTO Usuario(username, nombre, password, rol) VALUES (?,?,?,?)";
    try (Connection cn = Db.get()) {
      try (PreparedStatement ps = cn.prepareStatement(check)) {
        ps.setString(1, username);
        try (ResultSet rs = ps.executeQuery()) {
          if (rs.next()) throw new IllegalStateException("El usuario ya existe.");
        }
      }
      try (PreparedStatement ps = cn.prepareStatement(ins, Statement.RETURN_GENERATED_KEYS)) {
        ps.setString(1, username);
        ps.setString(2, nombre);
        ps.setString(3, passwordPlano);
        ps.setString(4, rol);
        ps.executeUpdate();
        try (ResultSet gk = ps.getGeneratedKeys()) { if (gk.next()) return gk.getInt(1); }
      }
      throw new IllegalStateException("No se obtuvo id del usuario.");
    } catch (SQLException e) {
      throw new RuntimeException("Error creando usuario: " + e.getMessage(), e);
    }
  }

  /** Busca un usuario por username (o null si no existe) */
  public User findByUsername(String username) {
    String sql = "SELECT id, username, nombre, rol FROM Usuario WHERE username = ?";
    try (Connection cn = Db.get();
         PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setString(1, username);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return new User(
              rs.getInt("id"),
              rs.getString("username"),
              rs.getString("nombre"),
              rs.getString("rol")
          );
        }
        return null;
      }
    } catch (SQLException e) {
      throw new RuntimeException("Error buscando usuario", e);
    }
  }

  /** Verifica el password (PLANO por ahora, para demo) */
  public boolean passwordOk(String username, String passwordPlano) {
    String sql = "SELECT password FROM Usuario WHERE username = ?";
    try (Connection cn = Db.get();
         PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setString(1, username);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) return false;
        String passDb = rs.getString("password");
        return passDb != null && passDb.equals(passwordPlano);
      }
    } catch (SQLException e) {
      throw new RuntimeException("Error verificando password", e);
    }
  }

  /** Devuelve la contraseña tal como está almacenada (para tu “olvidé mi contraseña”). */
  public String obtenerPasswordPlano(String username) {
    String sql = "SELECT password FROM Usuario WHERE username = ?";
    try (Connection cn = Db.get(); PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setString(1, username);
      try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getString(1) : null; }
    } catch (SQLException e) {
      throw new RuntimeException("Error leyendo contraseña", e);
    }
  }

  /** Crea una sesión 'Activa' y devuelve su id */
  public int abrirSesion(int usuarioId) {
    String sql = "INSERT INTO Sesion(usuario_id, estado) VALUES (?, 'Activa')";
    try (Connection cn = Db.get();
         PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      ps.setInt(1, usuarioId);
      ps.executeUpdate();
      try (ResultSet gk = ps.getGeneratedKeys()) { if (gk.next()) return gk.getInt(1); }
      throw new IllegalStateException("No se obtuvo id de sesión");
    } catch (SQLException e) {
      throw new RuntimeException("Error abriendo sesión", e);
    }
  }

  /** Marca la sesión como 'Cerrada' */
  public void cerrarSesion(int sesionId) {
    String sql = "UPDATE Sesion SET estado='Cerrada' WHERE id=?";
    try (Connection cn = Db.get(); PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setInt(1, sesionId);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("Error cerrando sesión", e);
    }
  }

  /* ====== Nuevos: resolver ids enlazados por usuario ====== */

  /** Devuelve paciente_id por usuario_id, o null si no está enlazado. */
  public Integer pacienteIdPorUsuario(int usuarioId) {
    String sql = "SELECT id FROM Paciente WHERE usuario_id = ?";
    try (Connection cn = Db.get(); PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setInt(1, usuarioId);
      try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : null; }
    } catch (SQLException e) {
      throw new RuntimeException("Error resolviendo paciente por usuario", e);
    }
  }

  /** Devuelve doctor_id por usuario_id, o null si no está enlazado. */
  public Integer doctorIdPorUsuario(int usuarioId) {
    String sql = "SELECT id FROM Doctor WHERE usuario_id = ?";
    try (Connection cn = Db.get(); PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setInt(1, usuarioId);
      try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : null; }
    } catch (SQLException e) {
      throw new RuntimeException("Error resolviendo doctor por usuario", e);
    }
  }
}
