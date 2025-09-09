package bdpryfinal;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SesionDaoJdbc {

  /** Proyección mínima de la sesión. */
  public static class Sesion {
    public final int id;
    public final int usuarioId;
    public final LocalDateTime instanteInicio;
    public final String estado;

    public Sesion(int id, int usuarioId, LocalDateTime instanteInicio, String estado) {
      this.id = id;
      this.usuarioId = usuarioId;
      this.instanteInicio = instanteInicio;
      this.estado = estado;
    }

    @Override public String toString() {
      return "Sesion{id=%d, usuarioId=%d, inicio=%s, estado=%s}"
          .formatted(id, usuarioId, instanteInicio, estado);
    }
  }

  /** Abre una sesión para el usuario y devuelve el id generado. */
  public int abrirSesion(int usuarioId) {
    final String sql =
        "INSERT INTO Sesion(usuario_id, instante_inicio, estado) VALUES (?, NOW(), 'Activa')";
    try (Connection con = Db.get();
         PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      ps.setInt(1, usuarioId);
      ps.executeUpdate();
      try (ResultSet rs = ps.getGeneratedKeys()) {
        rs.next();
        return rs.getInt(1);
      }
    } catch (SQLException e) {
      throw new RuntimeException("Error abriendo sesión para usuarioId=" + usuarioId, e);
    }
  }

  /** Marca la sesión como 'Cerrada'. */
  public void cerrarSesion(int sesionId) {
    final String sql = "UPDATE Sesion SET estado='Cerrada' WHERE id=?";
    try (Connection con = Db.get();
         PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setInt(1, sesionId);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("Error cerrando sesión id=" + sesionId, e);
    }
  }

  /** Devuelve las últimas N sesiones de un usuario. */
  public List<Sesion> ultimasDelUsuario(int usuarioId, int limit) {
    final String sql =
        "SELECT id, usuario_id, instante_inicio, estado " +
        "FROM Sesion WHERE usuario_id=? ORDER BY instante_inicio DESC LIMIT ?";
    List<Sesion> out = new ArrayList<>();
    try (Connection con = Db.get();
         PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setInt(1, usuarioId);
      ps.setInt(2, limit);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          out.add(new Sesion(
              rs.getInt("id"),
              rs.getInt("usuario_id"),
              rs.getTimestamp("instante_inicio").toLocalDateTime(),
              rs.getString("estado")
          ));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Error listando sesiones de usuarioId=" + usuarioId, e);
    }
    return out;
  }
}
