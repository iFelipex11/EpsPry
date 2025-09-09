package bdpryfinal;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DoctorDaoJdbc {

  /** Item ligero para UI (ComboBox) */
  public static final class DoctorItem {
    public final int id;
    public final String identificacion; // código (DOC-001, etc.)
    public final String nombre;
    public final String especialidad;

    public DoctorItem(int id, String identificacion, String nombre, String especialidad) {
      this.id = id;
      this.identificacion = identificacion;
      this.nombre = nombre;
      this.especialidad = especialidad;
    }

    @Override public String toString() {
      return nombre + (especialidad == null || especialidad.isBlank() ? "" : " (" + especialidad + ")")
          + " [" + identificacion + "]";
    }
  }

  /** Devuelve todos los doctores ordenados por nombre */
  public List<DoctorItem> listarTodos() {
    String sql = "SELECT id, identificacion, nombre, especialidad FROM Doctor ORDER BY nombre";
    List<DoctorItem> out = new ArrayList<>();
    try (Connection cn = Db.get();
         PreparedStatement ps = cn.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {

      while (rs.next()) {
        out.add(new DoctorItem(
            rs.getInt("id"),
            rs.getString("identificacion"),
            rs.getString("nombre"),
            rs.getString("especialidad")
        ));
      }
      return out;
    } catch (SQLException e) {
      throw new RuntimeException("Error listando doctores", e);
    }
  }
}
