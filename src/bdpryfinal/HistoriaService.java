package bdpryfinal;

import java.util.List;

public class HistoriaService {

  public static final class HistoriaView {
    public final PacienteDaoJdbc.PacienteItem paciente;
    public final int historiaId;
    public final List<HistoriaDaoJdbc.Nota> notas;
    public HistoriaView(PacienteDaoJdbc.PacienteItem p, int historiaId, List<HistoriaDaoJdbc.Nota> notas) {
      this.paciente = p; this.historiaId = historiaId; this.notas = notas;
    }
  }

  private final PacienteDaoJdbc pacDao = new PacienteDaoJdbc();
  private final HistoriaDaoJdbc histDao = new HistoriaDaoJdbc();

  /** Abre (o crea) la historia por código de paciente y lista sus notas. */
  public HistoriaView abrirPorCodigoPaciente(String codPaciente) {
    if (codPaciente == null || codPaciente.isBlank())
      throw new IllegalArgumentException("Código de paciente requerido");
    var p = pacDao.findByCodigo(codPaciente);
    if (p == null) throw new IllegalArgumentException("Paciente no encontrado");
    int hid = histDao.getOrCreateHistoriaId(p.id);
    var notas = histDao.listarNotas(hid);
    return new HistoriaView(p, hid, notas);
  }

  /** Compatibilidad: agrega nota pasando solo 'texto' (se guarda como motivo_consulta). */
  public HistoriaView agregarNotaPorCodigo(String codPaciente, String texto) {
    if (texto == null || texto.isBlank())
      throw new IllegalArgumentException("Texto requerido");
    var p = pacDao.findByCodigo(codPaciente);
    if (p == null) throw new IllegalArgumentException("Paciente no encontrado");
    int hid = histDao.getOrCreateHistoriaId(p.id);
    histDao.agregarNotaSoloTexto(hid, texto);
    var notas = histDao.listarNotas(hid);
    return new HistoriaView(p, hid, notas);
  }

  /** Agrega nota completa (sin 'texto') y devuelve la vista actualizada. */
  public HistoriaView agregarNotaPorCodigo(String codPaciente,
                                           String alergias,
                                           String medicamentos,
                                           String motivoConsulta,
                                           String recomendaciones) {
    var p = pacDao.findByCodigo(codPaciente);
    if (p == null) throw new IllegalArgumentException("Paciente no encontrado");
    int hid = histDao.getOrCreateHistoriaId(p.id);
    histDao.agregarNota(hid, alergias, medicamentos, motivoConsulta, recomendaciones);
    var notas = histDao.listarNotas(hid);
    return new HistoriaView(p, hid, notas);
  }
}
