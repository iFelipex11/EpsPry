package bdpryfinal;

import java.util.List;

public class NotaService {

  public static final class NotaView {
    public final PacienteDaoJdbc.PacienteItem paciente;
    public final int notaId;
    public final List<NotaDaoJdbc.NotaTxt> notas;
    public NotaView(PacienteDaoJdbc.PacienteItem p, int notaId, List<NotaDaoJdbc.NotaTxt> notas) {
      this.paciente = p; this.notaId = notaId; this.notas = notas;
    }
  }

  private final PacienteDaoJdbc pacDao = new PacienteDaoJdbc();
  private final NotaDaoJdbc notaDao = new NotaDaoJdbc();

  /** Abre (o crea) la 'nota' del paciente y lista sus entradas de texto. */
  public NotaView abrirPorCodigoPaciente(String codPaciente) {
    if (codPaciente == null || codPaciente.isBlank())
      throw new IllegalArgumentException("Código de paciente requerido");
    var p = pacDao.findByCodigo(codPaciente.trim());
    if (p == null) throw new IllegalArgumentException("Paciente no encontrado");
    int notaId = notaDao.getOrCreateNotaId(p.id);
    var notas = notaDao.listarNotas(notaId);
    return new NotaView(p, notaId, notas);
  }

  /** Agrega una entrada de texto y devuelve la vista actualizada. */
  public NotaView agregarNotaPorCodigo(String codPaciente, String texto) {
    if (texto == null || texto.isBlank())
      throw new IllegalArgumentException("Texto requerido");
    var p = pacDao.findByCodigo(codPaciente.trim());
    if (p == null) throw new IllegalArgumentException("Paciente no encontrado");
    int notaId = notaDao.getOrCreateNotaId(p.id);
    notaDao.agregarNota(notaId, texto);
    var notas = notaDao.listarNotas(notaId);
    return new NotaView(p, notaId, notas);
  }
}
