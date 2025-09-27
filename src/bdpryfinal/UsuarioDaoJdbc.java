package bdpryfinal;

import java.sql.*;


public class UsuarioDaoJdbc {

  //Clase anidada user
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


  //Agrega una fila a la tabla username con un usuario nuevo
  public int crearUsuario(String username, String passwordPlano, String rol) {
    final String check = "SELECT 1 FROM Usuario WHERE username = ?"; //Devolvemos 1 cuando existe un usuario
    final String ins   = "INSERT INTO Usuario(username, password, rol) VALUES (?,?,?)"; //Insertamos un nuevo usuario
    try (Connection cn = Db.get()) {
      // Unicidad de username
      try (PreparedStatement ps = cn.prepareStatement(check)) {
        ps.setString(1, username);
        try (ResultSet rs = ps.executeQuery()) { //Hacemos una consulta y pasamos el valor username para saber si existe
          if (rs.next()) throw new IllegalStateException("El usuario ya existe.");
        }
      }
      //Creamos un nuevo usuario y obtenemos la clave autogenerada por mysql
      try (PreparedStatement ps = cn.prepareStatement(ins, Statement.RETURN_GENERATED_KEYS)) {
        ps.setString(1, username);
        ps.setString(2, passwordPlano); //Usamos una password en plano para que sea mas facil obtener su valor
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


  public User EncontrarPorUser(String username) {
    final String sql = //Hacemos una query para que nos retorne todos los datos de un usuario cuando se encuentre
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
      throw new RuntimeException("Error buscando usuario", e); //Si hay error al establecer la conexion llama a este metodo
    }
  }

  //Verificamos la contraseña de un usuario
  public boolean VerfPassword(String username, String passwordPlano) {
    final String sql = "SELECT password FROM Usuario WHERE username = ?";
    try (Connection cn = Db.get();
         PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setString(1, username);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) return false; // si no hay filas para mostrar entonces retorna flaso sino continua
        String passDb = rs.getString("password"); //Aca obtenemos la contraseña en plano
        return passDb != null && passDb.equals(passwordPlano); //En este apartado verificamos que la contraseña no sea nula y que sea igual a la que le pasamos
      }
    } catch (SQLException e) {
      throw new RuntimeException("Error verificando password", e);
    }
  }

  //
  public String obtenerPassword(String username) {
    final String sql = "SELECT password FROM Usuario WHERE username = ?";
    try (Connection cn = Db.get();
         PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setString(1, username);
      try (ResultSet rs = ps.executeQuery()) { //Hacemos la query
        return rs.next() ? rs.getString(1) : null; //Hacemos un operador ternario, si es true devuelve la contraseña sino devuelve null
      }
    } catch (SQLException e) {
      throw new RuntimeException("Error leyendo contraseña", e);
    }
  }

  /* ======================= Sesiones ======================= */

  //Abrimos sesion
  //Revisar a futuro para quitar o modificar
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

  //Abrimos sesion
  //Revisar a futuro para quitar o modificar
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

  // Devuelve paciente_id por usuario_id, o null si no está enlazado
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

  // Devuelve doctor_id por usuario_id, o null si no está enlazado.
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
