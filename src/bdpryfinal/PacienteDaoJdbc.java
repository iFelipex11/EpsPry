package bdpryfinal;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** DAO simple para Paciente (registro, búsquedas y utilidades). */
public class PacienteDaoJdbc {

  /** DTO ligero para mostrar/usar en listas */
  public static final class PacienteItem {
    public final int id;
    public final String cedula;   // ← antes 'identificacion'
    public final String nombre;   // nombre completo armado

    public PacienteItem(int id, String cedula, String nombre) {
      this.id = id;
      this.cedula = cedula;
      this.nombre = nombre;
    }

    @Override public String toString() { return nombre + " [" + cedula + "]"; }
  }

  /* ===================== Registro / Perfil ===================== */

  public void insertarPerfilPaciente(int usuarioId,
                                     String cedula,
                                     String nombre1,
                                     String nombre2,
                                     String apellido1,
                                     String apellido2,
                                     String correo,
                                     String telefono,
                                     String genero,
                                     String direccion,
                                     java.time.LocalDate fechaNacimiento) {

    final String sql =
        "INSERT INTO Paciente (" +
        "  cedula, nombre1, nombre2, apellido1, apellido2, " +
        "  correo, telefono, genero, direccion, fecha_nacimiento, usuario_id" +
        ") VALUES (?,?,?,?,?,?,?,?,?,?,?) " +
        "ON DUPLICATE KEY UPDATE " +
        "  cedula=VALUES(cedula), " +
        "  nombre1=VALUES(nombre1), " +
        "  nombre2=VALUES(nombre2), " +
        "  apellido1=VALUES(apellido1), " +
        "  apellido2=VALUES(apellido2), " +
        "  correo=VALUES(correo), " +
        "  telefono=VALUES(telefono), " +
        "  genero=VALUES(genero), " +
        "  direccion=VALUES(direccion), " +
        "  fecha_nacimiento=VALUES(fecha_nacimiento)";

    try (Connection cn = Db.get();
         PreparedStatement ps = cn.prepareStatement(sql)) {

      ps.setString(1,  blankToNull(cedula));
      ps.setString(2,  blankToNull(nombre1));
      ps.setString(3,  blankToNull(nombre2));
      ps.setString(4,  blankToNull(apellido1));
      ps.setString(5,  blankToNull(apellido2));
      ps.setString(6,  blankToNull(correo));
      ps.setString(7,  blankToNull(telefono));
      ps.setString(8,  blankToNull(genero));
      ps.setString(9,  blankToNull(direccion));
      if (fechaNacimiento != null) ps.setDate(10, Date.valueOf(fechaNacimiento));
      else                         ps.setNull(10, Types.DATE);
      ps.setInt(11, usuarioId);

      ps.executeUpdate();

    } catch (SQLException e) {
      throw new RuntimeException("Error insertando/actualizando perfil Paciente", e);
    }
  }

  public void completarPerfilPorUsuarioId(int usuarioId,
                                          java.time.LocalDate fechaNacimiento,
                                          String genero, String telefono,
                                          String direccion, String correo) {
    final String sql =
        "UPDATE Paciente SET fecha_nacimiento=?, genero=?, telefono=?, direccion=?, correo=? " +
        "WHERE usuario_id=?";
    try (Connection con = Db.get();
         PreparedStatement ps = con.prepareStatement(sql)) {

      if (fechaNacimiento != null) ps.setDate(1, Date.valueOf(fechaNacimiento));
      else                         ps.setNull(1, Types.DATE);
      ps.setString(2, blankToNull(genero));
      ps.setString(3, blankToNull(telefono));
      ps.setString(4, blankToNull(direccion));
      ps.setString(5, blankToNull(correo));
      ps.setInt(6, usuarioId);

      int rows = ps.executeUpdate();
      if (rows == 0) throw new IllegalStateException("No existe perfil Paciente para usuarioId=" + usuarioId);
    } catch (SQLException e) {
      throw new RuntimeException("Error actualizando perfil Paciente (usuarioId=" + usuarioId + ")", e);
    }
  }

  /* ===================== Consultas útiles ===================== */

  /** Busca el ID por “código” de paciente: ahora es su CÉDULA. */
  public static int EncontrarIdPorCodigo(String codigoPaciente) {
    if (codigoPaciente == null || codigoPaciente.isBlank())
      throw new IllegalArgumentException("Código de paciente (cédula) requerido");
    String sql = "SELECT id FROM Paciente WHERE cedula = ?";
    try (Connection cn = Db.get();
         PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setString(1, codigoPaciente.trim());
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) return rs.getInt(1);
      }
      throw new IllegalArgumentException("Paciente no encontrado: " + codigoPaciente);
    } catch (SQLException e) {
      throw new RuntimeException("Error buscando paciente por cédula: " + e.getMessage(), e);
    }
  }

  /** Devuelve un item por cédula, o null si no existe. */
  public PacienteItem EncontrarPorCodigo(String codigoPaciente) {
    String sql =
        "SELECT id, cedula, nombre1, nombre2, apellido1, apellido2 " +
        "FROM Paciente WHERE cedula = ?";
    try (Connection cn = Db.get();
         PreparedStatement ps = cn.prepareStatement(sql)) {
      String cod = codigoPaciente == null ? "" : codigoPaciente.trim();
      ps.setString(1, cod);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          String nombre = UnirStrings(
              rs.getString("nombre1"),
              rs.getString("nombre2"),
              rs.getString("apellido1"),
              rs.getString("apellido2")
          );
          return new PacienteItem(rs.getInt("id"), rs.getString("cedula"), nombre);
        }
        return null;
      }
    } catch (SQLException e) {
      throw new RuntimeException("Error consultando paciente", e);
    }
  }

  /** Búsqueda por nombre “completo” */
  public List<PacienteItem> buscarPorNombre(String filtro) {
    String sql = "SELECT id, cedula, " +
                 "CONCAT_WS(' ', nombre1, nombre2, apellido1, apellido2) AS nombre_comp " +
                 "FROM Paciente " +
                 "WHERE CONCAT_WS(' ', nombre1, nombre2, apellido1, apellido2) LIKE ? " +
                 "ORDER BY nombre_comp";
    List<PacienteItem> pacientes = new ArrayList<>();
    try (Connection cn = Db.get();
         PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setString(1, "%" + (filtro == null ? "" : filtro.trim()) + "%");
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          pacientes.add(new PacienteItem(
              rs.getInt("id"),
              rs.getString("cedula"),
              rs.getString("nombre_comp")
          ));
        }
      }
      return pacientes;
    } catch (SQLException e) {
      throw new RuntimeException("Error buscando pacientes", e);
    }
  }

  /* ===================== Helpers ===================== */

  private static String UnirStrings(String... parts) {
    StringBuilder sb = new StringBuilder();
    for (String p : parts) {
      if (p != null) {
        String t = p.trim();
        if (!t.isEmpty()) {
          if (sb.length() > 0) sb.append(' ');
          sb.append(t);
        }
      }
    }
    return sb.toString();
  }

  private static String blankToNull(String s) {
    if (s == null) return null;
    String t = s.trim();
    return t.isEmpty() ? null : t;
  }
}
