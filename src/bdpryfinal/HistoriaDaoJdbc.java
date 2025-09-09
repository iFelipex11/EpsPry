package bdpryfinal;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class HistoriaDaoJdbc {

  /** Nota de historia clínica */
  public static final class Nota {
    public final int id;
    public final String texto;
    public final Timestamp creadaEn;
    public Nota(int id, String texto, Timestamp creadaEn) {
      this.id = id; this.texto = texto; this.creadaEn = creadaEn;
    }
  }

  /** Devuelve id de historia; si no existe, la crea. */
  public int getOrCreateHistoriaId(int pacienteId) {
    String sel = "SELECT id FROM Historia_Clinica WHERE paciente_id = ?";
    String ins = "INSERT INTO Historia_Clinica(paciente_id) VALUES (?)";
    try (Connection cn = Db.get()) {
      // buscar
      try (PreparedStatement ps = cn.prepareStatement(sel)) {
        ps.setInt(1, pacienteId);
        try (ResultSet rs = ps.executeQuery()) {
          if (rs.next()) return rs.getInt(1);
        }
      }
      // crear
      try (PreparedStatement ps = cn.prepareStatement(ins, Statement.RETURN_GENERATED_KEYS)) {
        ps.setInt(1, pacienteId);
        ps.executeUpdate();
        try (ResultSet gk = ps.getGeneratedKeys()) {
          if (gk.next()) return gk.getInt(1);
        }
      }
      throw new IllegalStateException("No se obtuvo id de Historia_Clinica");
    } catch (SQLException e) {
      throw new RuntimeException("Error obteniendo/creando historia", e);
    }
  }

  /** Lista notas (más recientes primero). */
  public List<Nota> listarNotas(int historiaId) {
    String sql = "SELECT id, texto, creada_en FROM Historia_Nota WHERE historia_id=? ORDER BY creada_en DESC, id DESC";
    List<Nota> out = new ArrayList<>();
    try (Connection cn = Db.get();
         PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setInt(1, historiaId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          out.add(new Nota(
              rs.getInt("id"),
              rs.getString("texto"),
              rs.getTimestamp("creada_en")
          ));
        }
      }
      return out;
    } catch (SQLException e) {
      throw new RuntimeException("Error listando notas", e);
    }
  }

  /** Agrega una nota a la historia. */
  public int agregarNota(int historiaId, String texto) {
    String sql = "INSERT INTO Historia_Nota(historia_id, texto) VALUES (?,?)";
    try (Connection cn = Db.get();
         PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      ps.setInt(1, historiaId);
      ps.setString(2, texto);
      ps.executeUpdate();
      try (ResultSet gk = ps.getGeneratedKeys()) {
        if (gk.next()) return gk.getInt(1);
      }
      throw new IllegalStateException("No se obtuvo id de nota");
    } catch (SQLException e) {
      throw new RuntimeException("Error agregando nota", e);
    }
  }
}
