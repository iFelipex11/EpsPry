package bdpryfinal;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SesionDaoJdbc {

  //Objeto que maneja las sesiones
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

  //Creamos una sesion con el id del usuario
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

  //Cerramos la sesion por el idsesion
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
}
