package bdpryfinal;

import java.util.List;

public class NotaService {
    //Clase anidada para crear una nota con su respectivo paciente
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

  //
  public NotaView abrirPorCodigoPaciente(String codPaciente) {
    if (codPaciente == null || codPaciente.isBlank())
      throw new IllegalArgumentException("Código de paciente requerido");
    // Con var evitamos pasarle el tipo de dato
    var p = pacDao.EncontrarPorCodigo(codPaciente.trim()); //
    if (p == null) throw new IllegalArgumentException("Paciente no encontrado");
    //Creamos la nota
    int notaId = notaDao.CrearNotaId(p.id);
    var notas = notaDao.listarNotas(notaId);
    //Creamos un objeto NotaView con el paciente, el idNota y la lista de notas
    return new NotaView(p, notaId, notas);
  }

  //Pasamos el codigo del paciente y un texto
  public NotaView agregarNotaPorCodigo(String codPaciente, String texto) {
    if (texto == null || texto.isBlank())
      throw new IllegalArgumentException("Texto requerido");
    var p = pacDao.EncontrarPorCodigo(codPaciente.trim());
    if (p == null) throw new IllegalArgumentException("Paciente no encontrado");
    int notaId = notaDao.CrearNotaId(p.id);
    notaDao.agregarNota(notaId, texto);
    var notas = notaDao.listarNotas(notaId);
    //Retornamos un objeto NotaView
    return new NotaView(p, notaId, notas);
  }
}
