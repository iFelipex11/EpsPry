package pfx;

import bdpryfinal.UsuarioDaoJdbc;

public class SessionContext {
  public static UsuarioDaoJdbc.User currentUser;
  public static Integer currentSesionId;

  // Enlazados por login según rol:
  public static Integer pacienteId; // si rol = Paciente
  public static Integer doctorId;   // si rol = Doctor

  public static void clear() {
    currentUser = null;
    currentSesionId = null;
    pacienteId = null;
    doctorId = null;
  }
}
