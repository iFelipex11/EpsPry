package bdpryfinal;

import pfx.SessionContext;

/** Servicio de autenticación */
public class AuthService {

  //Clase anidada "constante" para almacenar un login
  public record LoginResult(UsuarioDaoJdbc.User user, int sesionId, Integer pacienteId, Integer doctorId) {}

  private final UsuarioDaoJdbc usuarioDao = new UsuarioDaoJdbc();

  // Ingresamos con un usuario y contraseña
  public LoginResult login(String username, String password) {
    if (username == null || username.isBlank()) throw new IllegalArgumentException("Usuario requerido");
    if (password == null) password = "";

    var u = usuarioDao.EncontrarPorUser(username); //Asignamos el usuario dado a la variable u
    if (u == null) throw new IllegalArgumentException("Credenciales inválidas");
    if (!usuarioDao.VerfPassword(username, password)) throw new IllegalArgumentException("Credenciales inválidas"); //En caso de que VerfPassword retorne falso va a soltar error

    int sesionId = usuarioDao.abrirSesion(u.id); //Abrimos una sesion para el usuario en cuestion y pasamos el id de la sesion a la variable sesionId
    Integer pid = null, did = null; //Inicializamos tanto el id del paciente como del doctor como nulos

    if ("Paciente".equalsIgnoreCase(u.rol)) { //En caso de que el rol sea paciente(sin importar las mayusculas y minusculas) pid sera igual al id del usuario
      pid = usuarioDao.pacienteIdPorUsuario(u.id);
    } else if ("Doctor".equalsIgnoreCase(u.rol)) { //En caso de que el rol sea doctor(sin importar las mayusculas y minusculas) did sera igual al id del usuario
      did = usuarioDao.doctorIdPorUsuario(u.id); // opcional si has enlazado doctores
    }

    // guardar en SessionContext para toda la app
    SessionContext.currentUser = u;
    SessionContext.currentSesionId = sesionId;
    SessionContext.pacienteId = pid;
    SessionContext.doctorId = did;

    return new LoginResult(u, sesionId, pid, did);
  }

  // Cuando hacemos el logout pasaoms unicamente el id de la sesion y llamamos al metodo del usuario cerrarSesion
  public void logout(Integer sesionId) {
    if (sesionId == null) return;
    try { usuarioDao.cerrarSesion(sesionId); } catch (Exception ignore) {}
  }
}
