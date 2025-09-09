package bdpryfinal;

import pfx.SessionContext;

/** Servicio de autenticación */
public class AuthService {

  public record LoginResult(UsuarioDaoJdbc.User user, int sesionId, Integer pacienteId, Integer doctorId) {}

  private final UsuarioDaoJdbc usuarioDao = new UsuarioDaoJdbc();

  /** Login con username/password plano. Carga ids enlazados y setea SessionContext. */
  public LoginResult login(String username, String password) {
    if (username == null || username.isBlank()) throw new IllegalArgumentException("Usuario requerido");
    if (password == null) password = "";

    var u = usuarioDao.findByUsername(username);
    if (u == null) throw new IllegalArgumentException("Credenciales inválidas");
    if (!usuarioDao.passwordOk(username, password)) throw new IllegalArgumentException("Credenciales inválidas");

    int sesionId = usuarioDao.abrirSesion(u.id);
    Integer pid = null, did = null;

    if ("Paciente".equalsIgnoreCase(u.rol)) {
      pid = usuarioDao.pacienteIdPorUsuario(u.id);
    } else if ("Doctor".equalsIgnoreCase(u.rol)) {
      did = usuarioDao.doctorIdPorUsuario(u.id); // opcional si has enlazado doctores
    }

    // guardar en SessionContext para toda la app
    SessionContext.currentUser = u;
    SessionContext.currentSesionId = sesionId;
    SessionContext.pacienteId = pid;
    SessionContext.doctorId = did;

    return new LoginResult(u, sesionId, pid, did);
  }

  /** Cierra sesión por id (ignora errores menores). */
  public void logout(Integer sesionId) {
    if (sesionId == null) return;
    try { usuarioDao.cerrarSesion(sesionId); } catch (Exception ignore) {}
  }
}
