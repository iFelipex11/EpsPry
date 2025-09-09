package bdpryfinal;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** DAO JDBC para tabla Cita */
public class CitaDaoJdbc {

  /* ====== DTOs ligeros ====== */
  public static final class AgendaItem {
    public final int id;
    public final String hora;
    public final String paciente;
    public final String estado;
    public final String observacion;
    public AgendaItem(int id, String hora, String paciente, String estado, String observacion) {
      this.id = id; this.hora = hora; this.paciente = paciente; this.estado = estado; this.observacion = observacion;
    }
  }

  public static final class CitaPacItem {
    public final int id;
    public final String fecha;
    public final String hora;
    public final String doctor;
    public final String estado;
    public final String observacion;
    public CitaPacItem(int id, String fecha, String hora, String doctor, String estado, String obs) {
      this.id = id; this.fecha = fecha; this.hora = hora; this.doctor = doctor; this.estado = estado; this.observacion = obs;
    }
  }

  /* ====== Médico ====== */
  public List<AgendaItem> agendaDeDoctor(String codDoctor, LocalDate fecha) {
    String sql = """
      SELECT c.id,
             DATE_FORMAT(c.hora, '%H:%i') AS hhmm,
             p.nombre AS paciente,
             c.estado,
             c.observacion
      FROM Cita c
      JOIN Doctor d ON d.id = c.doctor_id
      JOIN Paciente p ON p.id = c.paciente_id
      WHERE d.identificacion = ? AND c.fecha = ?
      ORDER BY c.hora
      """;
    List<AgendaItem> out = new ArrayList<>();
    try (Connection cn = Db.get(); PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setString(1, codDoctor);
      ps.setDate(2, Date.valueOf(fecha));
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          out.add(new AgendaItem(
              rs.getInt("id"),
              rs.getString("hhmm"),
              rs.getString("paciente"),
              rs.getString("estado"),
              rs.getString("observacion")
          ));
        }
      }
      return out;
    } catch (SQLException e) {
      throw new RuntimeException("Error listando agenda: " + e.getMessage(), e);
    }
  }

  public int crear(int pacienteId, int doctorId, LocalDate fecha, LocalTime hora,
                   String estado, String observacion) {
    String sql = "INSERT INTO Cita(paciente_id, doctor_id, fecha, hora, estado, observacion) VALUES (?,?,?,?,?,?)";
    try (Connection cn = Db.get(); PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      ps.setInt(1, pacienteId);
      ps.setInt(2, doctorId);
      ps.setDate(3, Date.valueOf(fecha));
      ps.setTime(4, Time.valueOf(hora));
      ps.setString(5, estado);
      if (observacion == null || observacion.isBlank()) ps.setNull(6, Types.VARCHAR);
      else ps.setString(6, observacion);
      ps.executeUpdate();
      try (ResultSet gk = ps.getGeneratedKeys()) { if (gk.next()) return gk.getInt(1); }
      throw new IllegalStateException("No se obtuvo ID generado para la cita");
    } catch (SQLException e) {
      if ("23000".equals(e.getSQLState())) throw new IllegalStateException("Ya existe una cita para ese doctor en esa fecha y hora.", e);
      throw new RuntimeException("Error creando cita: " + e.getMessage(), e);
    }
  }

  public int crearPorCodigos(String codPaciente, String codDoctor, LocalDate fecha, LocalTime hora,
                             String estado, String observacion) {
    String find = """
      SELECT p.id AS pid, d.id AS did
      FROM Paciente p, Doctor d
      WHERE p.identificacion = ? AND d.identificacion = ?
      """;
    try (Connection cn = Db.get()) {
      cn.setAutoCommit(false);
      try (PreparedStatement ps = cn.prepareStatement(find)) {
        ps.setString(1, codPaciente);
        ps.setString(2, codDoctor);
        try (ResultSet rs = ps.executeQuery()) {
          if (!rs.next()) throw new IllegalArgumentException("Paciente o Doctor no encontrados por sus códigos.");
          int pid = rs.getInt("pid");
          int did = rs.getInt("did");
          String ins = "INSERT INTO Cita(paciente_id, doctor_id, fecha, hora, estado, observacion) VALUES (?,?,?,?,?,?)";
          try (PreparedStatement insPs = cn.prepareStatement(ins, Statement.RETURN_GENERATED_KEYS)) {
            insPs.setInt(1, pid);
            insPs.setInt(2, did);
            insPs.setDate(3, Date.valueOf(fecha));
            insPs.setTime(4, Time.valueOf(hora));
            insPs.setString(5, estado);
            if (observacion == null || observacion.isBlank()) insPs.setNull(6, Types.VARCHAR);
            else insPs.setString(6, observacion);
            insPs.executeUpdate();
            try (ResultSet gk = insPs.getGeneratedKeys()) {
              if (gk.next()) { cn.commit(); return gk.getInt(1); }
            }
            throw new IllegalStateException("No se obtuvo ID generado para la cita");
          }
        }
      } catch (SQLException ex) {
        cn.rollback();
        if ("23000".equals(ex.getSQLState())) throw new IllegalStateException("Ya existe una cita para ese doctor en esa fecha y hora.", ex);
        throw ex;
      } finally { try { cn.setAutoCommit(true); } catch (Exception ignore) {} }
    } catch (SQLException e) {
      throw new RuntimeException("Error creando cita: " + e.getMessage(), e);
    }
  }

  public void actualizarEstado(int citaId, String nuevoEstado) {
    String sql = "UPDATE Cita SET estado=? WHERE id=?";
    try (Connection cn = Db.get(); PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setString(1, nuevoEstado);
      ps.setInt(2, citaId);
      int n = ps.executeUpdate();
      if (n == 0) throw new IllegalArgumentException("Cita no encontrada (id=" + citaId + ")");
    } catch (SQLException e) {
      throw new RuntimeException("Error actualizando estado: " + e.getMessage(), e);
    }
  }

  /* ====== Paciente ====== */

  /** Lista citas del paciente entre fechas (ambos inclusive). Si from/to son null, no filtra ese extremo. */
  public List<CitaPacItem> citasDePaciente(int pacienteId, LocalDate from, LocalDate to) {
    StringBuilder sb = new StringBuilder("""
      SELECT c.id, DATE_FORMAT(c.fecha, '%Y-%m-%d') AS f,
             DATE_FORMAT(c.hora, '%H:%i') AS h,
             d.nombre AS doctor, c.estado, c.observacion
      FROM Cita c
      JOIN Doctor d ON d.id = c.doctor_id
      WHERE c.paciente_id = ?
      """);
    if (from != null) sb.append(" AND c.fecha >= ? ");
    if (to != null)   sb.append(" AND c.fecha <= ? ");
    sb.append(" ORDER BY c.fecha, c.hora ");

    List<CitaPacItem> out = new ArrayList<>();
    try (Connection cn = Db.get(); PreparedStatement ps = cn.prepareStatement(sb.toString())) {
      int i = 1;
      ps.setInt(i++, pacienteId);
      if (from != null) ps.setDate(i++, Date.valueOf(from));
      if (to != null)   ps.setDate(i++, Date.valueOf(to));
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          out.add(new CitaPacItem(
              rs.getInt("id"),
              rs.getString("f"),
              rs.getString("h"),
              rs.getString("doctor"),
              rs.getString("estado"),
              rs.getString("observacion")
          ));
        }
      }
      return out;
    } catch (SQLException e) {
      throw new RuntimeException("Error listando citas del paciente: " + e.getMessage(), e);
    }
  }

  /** Crear cita para paciente en sesión, estado Pendiente, resolviendo doctor por código. */
  public int crearPorPaciente(int pacienteId, String codDoctor, LocalDate fecha, LocalTime hora, String observacion) {
    String findDoc = "SELECT id FROM Doctor WHERE identificacion = ?";
    try (Connection cn = Db.get()) {
      int doctorId;
      try (PreparedStatement ps = cn.prepareStatement(findDoc)) {
        ps.setString(1, codDoctor);
        try (ResultSet rs = ps.executeQuery()) {
          if (!rs.next()) throw new IllegalArgumentException("Doctor no encontrado por código");
          doctorId = rs.getInt(1);
        }
      }
      return crear(pacienteId, doctorId, fecha, hora, "Pendiente", observacion);
    } catch (SQLException e) {
      throw new RuntimeException("Error creando cita para paciente: " + e.getMessage(), e);
    }
  }

  /** Cancelar cita solo si pertenece al paciente. */
  public void cancelarPorPaciente(int citaId, int pacienteId) {
    String sql = "UPDATE Cita SET estado='Cancelada' WHERE id=? AND paciente_id=? AND estado IN ('Pendiente','Confirmada')";
    try (Connection cn = Db.get(); PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setInt(1, citaId);
      ps.setInt(2, pacienteId);
      int n = ps.executeUpdate();
      if (n == 0) throw new IllegalArgumentException("No se pudo cancelar (no es tu cita o ya no es cancelable).");
    } catch (SQLException e) {
      throw new RuntimeException("Error cancelando cita: " + e.getMessage(), e);
    }
  }
}
