package bdpryfinal;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class HistoriaDaoJdbc {

  /** Nota de historia clínica: refleja exactamente Historia_Nota (sin 'texto'). */
  public static final class Nota {
    public final int id;
    public final int historiaId;
    public final String alergias;
    public final String medicamentos;
    public final String motivoConsulta;
    public final String recomendaciones;
    public final Timestamp creadaEn;

    public Nota(int id, int historiaId,
                String alergias, String medicamentos,
                String motivoConsulta, String recomendaciones,
                Timestamp creadaEn) {
      this.id = id; this.historiaId = historiaId;
      this.alergias = alergias; this.medicamentos = medicamentos;
      this.motivoConsulta = motivoConsulta; this.recomendaciones = recomendaciones;
      this.creadaEn = creadaEn;
    }
  }

  /** Devuelve id de historia; si no existe, la crea (útil incluso si hay trigger). */
  public int getOrCreateHistoriaId(int pacienteId) {
    final String sel = "SELECT id FROM Historia_Clinica WHERE paciente_id = ?";
    final String ins = "INSERT INTO Historia_Clinica(paciente_id) VALUES (?)";
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
    final String sql =
        "SELECT id, historia_id, alergias, medicamentos, motivo_consulta, recomendaciones, creada_en " +
        "FROM Historia_Nota WHERE historia_id=? " +
        "ORDER BY creada_en DESC, id DESC";
    List<Nota> out = new ArrayList<>();
    try (Connection cn = Db.get();
         PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setInt(1, historiaId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          out.add(new Nota(
              rs.getInt("id"),
              rs.getInt("historia_id"),
              rs.getString("alergias"),
              rs.getString("medicamentos"),
              rs.getString("motivo_consulta"),
              rs.getString("recomendaciones"),
              rs.getTimestamp("creada_en")
          ));
        }
      }
      return out;
    } catch (SQLException e) {
      throw new RuntimeException("Error listando notas", e);
    }
  }

  /** Inserta una nota (sin 'texto'). Devuelve el id generado. */
  public int agregarNota(int historiaId,
                         String alergias,
                         String medicamentos,
                         String motivoConsulta,
                         String recomendaciones) {
    if (historiaId <= 0) throw new IllegalArgumentException("historiaId inválido");

    final String sql =
        "INSERT INTO Historia_Nota (historia_id, alergias, medicamentos, motivo_consulta, recomendaciones) " +
        "VALUES (?, ?, ?, ?, ?)";
    try (Connection cn = Db.get();
         PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      ps.setInt(1, historiaId);
      setNullableVarchar(ps, 2, alergias);
      setNullableVarchar(ps, 3, medicamentos);
      setNullableVarchar(ps, 4, motivoConsulta);
      setNullableVarchar(ps, 5, recomendaciones);

      ps.executeUpdate();
      try (ResultSet gk = ps.getGeneratedKeys()) {
        if (gk.next()) return gk.getInt(1);
      }
      throw new IllegalStateException("No se obtuvo id de nota");
    } catch (SQLException e) {
      throw new RuntimeException("Error agregando nota", e);
    }
  }

  /** Compatibilidad opcional: si alguien aún pasa solo 'texto', lo guardamos como motivo_consulta. */
  public int agregarNotaSoloTexto(int historiaId, String texto) {
    String t = nullIfBlank(texto);
    return agregarNota(historiaId, null, null, t, null);
  }

  /* ===== helpers ===== */
  private static String nullIfBlank(String s) {
    if (s == null) return null;
    String t = s.trim();
    return t.isEmpty() ? null : t;
  }

  private static void setNullableVarchar(PreparedStatement ps, int idx, String val) throws SQLException {
    String t = nullIfBlank(val);
    if (t == null) ps.setNull(idx, Types.VARCHAR);
    else ps.setString(idx, t);
  }
}
