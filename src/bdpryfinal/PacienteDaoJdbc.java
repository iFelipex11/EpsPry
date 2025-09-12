package bdpryfinal;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** DAO simple para Paciente (lo necesario para mensajes y utilidades). */
public class PacienteDaoJdbc {

  /** DTO ligero para mostrar/usar en listas si lo necesitas */
  public static final class PacienteItem {
    public final int id;
    public final String identificacion;
    public final String nombre;

    public PacienteItem(int id, String identificacion, String nombre) {
      this.id = id;
      this.identificacion = identificacion;
      this.nombre = nombre;
    }

    @Override public String toString() { return nombre + " [" + identificacion + "]"; }
  }

  /** Busca el ID por código (columna `identificacion`). Lanza IllegalArgumentException si no existe. */
  public static int findIdByCodigo(String codigoPaciente) {
    if (codigoPaciente == null || codigoPaciente.isBlank())
      throw new IllegalArgumentException("Código de paciente requerido");
    String sql = "SELECT id FROM Paciente WHERE identificacion = ?";
    try (Connection cn = Db.get();
         PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setString(1, codigoPaciente.trim());
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) return rs.getInt(1);
      }
      throw new IllegalArgumentException("Paciente no encontrado: " + codigoPaciente);
    } catch (SQLException e) {
      throw new RuntimeException("Error buscando paciente por código: " + e.getMessage(), e);
    }
  }

  /** Devuelve un item por código o null si no existe. */
  public PacienteItem findByCodigo(String codigoPaciente) {
    String sql = "SELECT id, identificacion, nombre FROM Paciente WHERE identificacion = ?";
    try (Connection cn = Db.get();
         PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setString(1, codigoPaciente);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next())
          return new PacienteItem(rs.getInt("id"), rs.getString("identificacion"), rs.getString("nombre"));
        return null;
      }
    } catch (SQLException e) {
      throw new RuntimeException("Error consultando paciente", e);
    }
  }
  
  
  // Completar perfil de Paciente por usuario_id
public void completarPerfilPorUsuarioId(int usuarioId,
                                        java.time.LocalDate fechaNacimiento,
                                        String genero, String telefono, String direccion) {
  final String sql = "UPDATE Paciente SET fecha_nacimiento=?, genero=?, telefono=?, direccion=? WHERE usuario_id=?";
  try (java.sql.Connection con = Db.get();
       java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
    ps.setDate(1, (fechaNacimiento == null ? null : java.sql.Date.valueOf(fechaNacimiento)));
    ps.setString(2, genero);
    ps.setString(3, telefono);
    ps.setString(4, direccion);
    ps.setInt(5, usuarioId);
    int rows = ps.executeUpdate();
    if (rows == 0) throw new IllegalStateException("No existe perfil Paciente para usuarioId=" + usuarioId);
  } catch (java.sql.SQLException e) {
    throw new RuntimeException("Error actualizando perfil Paciente (usuarioId=" + usuarioId + ")", e);
  }
}


  /** Búsqueda opcional por nombre (por si la necesitas en algún cuadro de diálogo). */
  public List<PacienteItem> buscarPorNombre(String filtro) {
    String sql = "SELECT id, identificacion, nombre FROM Paciente WHERE nombre LIKE ? ORDER BY nombre";
    List<PacienteItem> out = new ArrayList<>();
    try (Connection cn = Db.get();
         PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setString(1, "%" + (filtro == null ? "" : filtro.trim()) + "%");
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next())
          out.add(new PacienteItem(rs.getInt("id"), rs.getString("identificacion"), rs.getString("nombre")));
      }
      return out;
    } catch (SQLException e) {
      throw new RuntimeException("Error buscando pacientes", e);
    }
  }
}
