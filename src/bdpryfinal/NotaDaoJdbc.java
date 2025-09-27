package bdpryfinal;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NotaDaoJdbc {

  //Clase anidada con nota
  public static final class NotaTxt {
    public final int id;
    public final int notaId;
    public final String texto;
    public final Timestamp creadaEn;
    public NotaTxt(int id, int notaId, String texto, Timestamp creadaEn) {
      this.id = id; this.notaId = notaId; this.texto = texto; this.creadaEn = creadaEn;
    }
  }

  // Creamos una nota en caso de que no exista, sino la busca
  public int CrearNotaId(int pacienteId) {
    final String sel = "SELECT id FROM Nota WHERE paciente_id=?"; //Selecionamos el id de nota donde el paciente tenga el id que le pasamos
    final String ins = "INSERT INTO Nota(paciente_id) VALUES(?)"; // Inserta la fila en Nota para este paciente (una por paciente)
    //solo se ejecuta si el SELECT previo no encontró registro

    try (Connection cn = Db.get()) {
      try (PreparedStatement ps = cn.prepareStatement(sel)) {
        ps.setInt(1, pacienteId);
        try (ResultSet rs = ps.executeQuery()) {
          if (rs.next()) return rs.getInt(1);
        }
      }
      try (PreparedStatement ps = cn.prepareStatement(ins, Statement.RETURN_GENERATED_KEYS)) { //Insertamos una nueva fila y 
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

  //Listamos las notas de forma descendente
  public List<NotaTxt> listarNotas(int notaId) {
    final String sql =
        "SELECT id, nota_id, texto, creada_en " +
        "FROM Paciente_Nota WHERE nota_id=? " +
        "ORDER BY creada_en DESC, id DESC";
    List<NotaTxt> Notastxt = new ArrayList<>();
    try (Connection cn = Db.get();
         PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setInt(1, notaId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          Notastxt.add(new NotaTxt(
              rs.getInt("id"),
              rs.getInt("nota_id"),
              rs.getString("texto"),
              rs.getTimestamp("creada_en")
          ));
        }
      }
      return Notastxt;
    } catch (SQLException e) {
      throw new RuntimeException("Error listando notas", e);
    }
  }

  //Agregamos una nueva nota
  public int agregarNota(int notaId, String texto) {
    if (texto == null || texto.isBlank()) //Si el texto esta en blanco o es vacio entonces genera un error
      throw new IllegalArgumentException("El campo 'texto' es obligatorio");
    final String sql = "INSERT INTO Paciente_Nota(nota_id, texto) VALUES (?,?)"; //Ahora insertamos una nueva nota con un nuevo id y texto
    try (Connection cn = Db.get();
         PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) { //Pasamos el codigo junto con un nuevo key autogenerado creado por la bd
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
