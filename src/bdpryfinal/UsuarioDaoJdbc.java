package bdpryfinal;

import java.sql.*;

/**
 * DAO para Usuario y Sesion, y resolución de vínculos a Paciente/Doctor.
 * - La tabla Usuario NO tiene columna 'nombre'.
 * - El campo 'nombre' del DTO se resuelve con:
 *   Paciente/Doctor -> CONCAT_WS(...) o, si no existe, username.
 */
public class UsuarioDaoJdbc {

  /** DTO de usuario para usar en la app */
  public static final class User {
    public final int id;
    public final String username;
    public final String nombre;  // nombre "mostrable" (paciente/doctor o username)
    public final String rol;

    public User(int id, String username, String nombre, String rol) {
      this.id = id;
      this.username = username;
      this.nombre = nombre;
      this.rol = rol;
    }
  }

  /* ======================= Creación / existencia ======================= */

  /** Crea usuario (tabla Usuario solo tiene username, password, rol). */
  public int crearUsuario(String username, String passwordPlano, String rol) {
    final String check = "SELECT 1 FROM Usuario WHERE username = ?";
    final String ins   = "INSERT INTO Usuario(username, password, rol) VALUES (?,?,?)";
    try (Connection cn = Db.get()) {
      // Unicidad de username
      try (PreparedStatement ps = cn.prepareStatement(check)) {
        ps.setString(1, username);
        try (ResultSet rs = ps.executeQuery()) {
          if (rs.next()) throw new IllegalStateException("El usuario ya existe.");
        }
      }
      // Insert
      try (PreparedStatement ps = cn.prepareStatement(ins, Statement.RETURN_GENERATED_KEYS)) {
        ps.setString(1, username);
        ps.setString(2, passwordPlano); // (opcional: aplicar hash antes)
        ps.setString(3, rol);
        ps.executeUpdate();
        try (ResultSet gk = ps.getGeneratedKeys()) {
          if (gk.next()) return gk.getInt(1);
        }
      }
      throw new IllegalStateException("No se obtuvo id del usuario.");
    } catch (SQLException e) {
      throw new RuntimeException("Error creando usuario: " + e.getMessage(), e);
    }
  }

  /** Overload legacy: acepta displayName pero lo ignora (no existe columna en BD). */
  public int crearUsuario(String username, String displayNameIgnorado, String passwordPlano, String rol) {
    return crearUsuario(username, passwordPlano, rol);
  }

  public boolean existeUsername(String username) {
    final String sql = "SELECT 1 FROM Usuario WHERE username = ?";
    try (Connection cn = Db.get();
         PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setString(1, username);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (SQLException e) {
      throw new RuntimeException("Error verificando username", e);
    }
  }

  /* ======================= Lectura / login ======================= */

  /**
   * Busca un usuario por username.
   * Resuelve 'nombre' con:
   *   COALESCE(CONCAT_WS(...) de Paciente, CONCAT_WS(...) de Doctor, username)
   */
  public User findByUsername(String username) {
    final String sql =
        "SELECT u.id, u.username, u.rol, " +
        "COALESCE(" +
        "  NULLIF(TRIM(CONCAT_WS(' ', p.nombre1,p.nombre2,p.apellido1,p.apellido2)), '')," +
        "  NULLIF(TRIM(CONCAT_WS(' ', d.nombre1,d.nombre2,d.apellido1,d.apellido2)), '')," +
        "  u.username" +
        ") AS nombre " +
        "FROM Usuario u " +
        "LEFT JOIN Paciente p ON p.usuario_id = u.id " +
        "LEFT JOIN Doctor   d ON d.usuario_id = u.id " +
        "WHERE u.username = ?";
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

  /** Verifica el password (PLANO por ahora, para demo). */
  public boolean passwordOk(String username, String passwordPlano) {
    final String sql = "SELECT password FROM Usuario WHERE username = ?";
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
    final String sql = "SELECT password FROM Usuario WHERE username = ?";
    try (Connection cn = Db.get();
         PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setString(1, username);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? rs.getString(1) : null;
      }
    } catch (SQLException e) {
      throw new RuntimeException("Error leyendo contraseña", e);
    }
  }

  /* ======================= Sesiones ======================= */

  /** Crea una sesión 'Activa' y devuelve su id. */
  public int abrirSesion(int usuarioId) {
    final String sql = "INSERT INTO Sesion(usuario_id, estado) VALUES (?, 'Activa')";
    try (Connection cn = Db.get();
         PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      ps.setInt(1, usuarioId);
      ps.executeUpdate();
      try (ResultSet gk = ps.getGeneratedKeys()) {
        if (gk.next()) return gk.getInt(1);
      }
      throw new IllegalStateException("No se obtuvo id de sesión");
    } catch (SQLException e) {
      throw new RuntimeException("Error abriendo sesión", e);
    }
  }

  /** Marca la sesión como 'Cerrada'. */
  public void cerrarSesion(int sesionId) {
    final String sql = "UPDATE Sesion SET estado='Cerrada' WHERE id=?";
    try (Connection cn = Db.get();
         PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setInt(1, sesionId);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("Error cerrando sesión", e);
    }
  }

  /* ======================= Resolución de vínculos ======================= */

  /** Devuelve paciente_id por usuario_id, o null si no está enlazado. */
  public Integer pacienteIdPorUsuario(int usuarioId) {
    final String sql = "SELECT id FROM Paciente WHERE usuario_id = ?";
    try (Connection cn = Db.get();
         PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setInt(1, usuarioId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? rs.getInt(1) : null;
      }
    } catch (SQLException e) {
      throw new RuntimeException("Error resolviendo paciente por usuario", e);
    }
  }

  /** Devuelve doctor_id por usuario_id, o null si no está enlazado. */
  public Integer doctorIdPorUsuario(int usuarioId) {
    final String sql = "SELECT id FROM Doctor WHERE usuario_id = ?";
    try (Connection cn = Db.get();
         PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setInt(1, usuarioId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? rs.getInt(1) : null;
      }
    } catch (SQLException e) {
      throw new RuntimeException("Error resolviendo doctor por usuario", e);
    }
  }
}
