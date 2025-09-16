package bdpryfinal;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** DAO para las notas simples (tablas: Nota y Paciente_Nota). */
public class NotaDaoJdbc {

  /** DTO: fila de Paciente_Nota */
  public static final class NotaTxt {
    public final int id;
    public final int notaId;
    public final String texto;
    public final Timestamp creadaEn;
    public NotaTxt(int id, int notaId, String texto, Timestamp creadaEn) {
      this.id = id; this.notaId = notaId; this.texto = texto; this.creadaEn = creadaEn;
    }
  }

  /** Devuelve id de Nota; si no existe para el paciente, la crea. */
  public int getOrCreateNotaId(int pacienteId) {
    final String sel = "SELECT id FROM Nota WHERE paciente_id=?";
    final String ins = "INSERT INTO Nota(paciente_id) VALUES(?)";
    try (Connection cn = Db.get()) {
      try (PreparedStatement ps = cn.prepareStatement(sel)) {
        ps.setInt(1, pacienteId);
        try (ResultSet rs = ps.executeQuery()) {
          if (rs.next()) return rs.getInt(1);
        }
      }
      try (PreparedStatement ps = cn.prepareStatement(ins, Statement.RETURN_GENERATED_KEYS)) {
        ps.setInt(1, pacienteId);
        ps.executeUpdate();
        try (ResultSet gk = ps.getGeneratedKeys()) {
          if (gk.next()) return gk.getInt(1);
        }
      }
      throw new IllegalStateException("No se obtuvo id de Nota");
    } catch (SQLException e) {
      throw new RuntimeException("Error obteniendo/creando Nota", e);
    }
  }

  /** Lista notas de texto (más recientes primero). */
  public List<NotaTxt> listarNotas(int notaId) {
    final String sql =
        "SELECT id, nota_id, texto, creada_en " +
        "FROM Paciente_Nota WHERE nota_id=? " +
        "ORDER BY creada_en DESC, id DESC";
    List<NotaTxt> out = new ArrayList<>();
    try (Connection cn = Db.get();
         PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setInt(1, notaId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          out.add(new NotaTxt(
              rs.getInt("id"),
              rs.getInt("nota_id"),
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

  /** Inserta una nueva nota de texto y devuelve su id generado. */
  public int agregarNota(int notaId, String texto) {
    if (texto == null || texto.isBlank())
      throw new IllegalArgumentException("El campo 'texto' es obligatorio");
    final String sql = "INSERT INTO Paciente_Nota(nota_id, texto) VALUES (?,?)";
    try (Connection cn = Db.get();
         PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      ps.setInt(1, notaId);
      ps.setString(2, texto.trim());
      ps.executeUpdate();
      try (ResultSet gk = ps.getGeneratedKeys()) { if (gk.next()) return gk.getInt(1); }
      throw new IllegalStateException("No se obtuvo id de nota");
    } catch (SQLException e) {
      throw new RuntimeException("Error agregando nota", e);
    }
  }
}
