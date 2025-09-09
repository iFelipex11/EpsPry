package bdpryfinal;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Fachada de negocio para Citas */
public class CitaService {

  private final CitaDaoJdbc citaDao = new CitaDaoJdbc();

  // Médico
  public List<CitaDaoJdbc.AgendaItem> agendaDeDoctor(String codDoctor, LocalDate fecha) {
    if (codDoctor == null || codDoctor.isBlank()) throw new IllegalArgumentException("Código de doctor requerido");
    if (fecha == null) throw new IllegalArgumentException("Fecha requerida");
    return citaDao.agendaDeDoctor(codDoctor, fecha);
  }
  public int crear(int pacienteId, int doctorId, LocalDate fecha, LocalTime hora, String estado, String obs) {
    validarCampos(fecha, hora, estado);
    return citaDao.crear(pacienteId, doctorId, fecha, hora, estado, obs);
  }
  public int crearPorCodigos(String codPaciente, String codDoctor, LocalDate fecha, LocalTime hora, String estado, String obs) {
    if (codPaciente == null || codPaciente.isBlank()) throw new IllegalArgumentException("Código de paciente requerido");
    if (codDoctor == null || codDoctor.isBlank()) throw new IllegalArgumentException("Código de doctor requerido");
    validarCampos(fecha, hora, estado);
    return citaDao.crearPorCodigos(codPaciente, codDoctor, fecha, hora, estado, obs);
  }
  public void actualizarEstado(int citaId, String nuevoEstado) {
    if (citaId <= 0) throw new IllegalArgumentException("Id inválido");
    if (nuevoEstado == null || nuevoEstado.isBlank()) throw new IllegalArgumentException("Estado requerido");
    citaDao.actualizarEstado(citaId, nuevoEstado);
  }

  // Paciente
  public List<CitaDaoJdbc.CitaPacItem> citasDePaciente(int pacienteId, LocalDate from, LocalDate to) {
    if (pacienteId <= 0) throw new IllegalArgumentException("pacienteId inválido");
    return citaDao.citasDePaciente(pacienteId, from, to);
  }
  public int crearPorPaciente(int pacienteId, String codDoctor, LocalDate fecha, LocalTime hora, String obs) {
    if (pacienteId <= 0) throw new IllegalArgumentException("pacienteId inválido");
    if (codDoctor == null || codDoctor.isBlank()) throw new IllegalArgumentException("Código de doctor requerido");
    if (fecha == null) throw new IllegalArgumentException("Fecha requerida");
    if (hora == null) throw new IllegalArgumentException("Hora requerida");
    return citaDao.crearPorPaciente(pacienteId, codDoctor, fecha, hora, obs);
  }
  public void cancelarPorPaciente(int citaId, int pacienteId) {
    if (citaId <= 0 || pacienteId <= 0) throw new IllegalArgumentException("Parámetros inválidos");
    citaDao.cancelarPorPaciente(citaId, pacienteId);
  }

  private void validarCampos(LocalDate fecha, LocalTime hora, String estado) {
    if (fecha == null) throw new IllegalArgumentException("Fecha requerida");
    if (hora == null) throw new IllegalArgumentException("Hora requerida");
    if (estado == null || estado.isBlank()) throw new IllegalArgumentException("Estado requerido");
  }
}
