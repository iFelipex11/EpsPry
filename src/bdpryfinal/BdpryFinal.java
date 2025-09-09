/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package bdpryfinal;

/**
 *
 * @author Whiterose
 */
public class BdpryFinal {
    /*
public static void main(String[] args) {
  var auth = new AuthService();
  var citaSrv = new CitaService();
  var udao = new UsuarioDaoJdbc();
  java.util.Scanner sc = new java.util.Scanner(System.in);

  UsuarioDaoJdbc.User usuarioActual = null;
  Integer sesionId = null;

  while (true) {
    System.out.println("\n================ MENÚ ================");
    if (usuarioActual == null) {
      System.out.println("1) Crear cuenta");
      System.out.println("2) Iniciar sesión");
      System.out.println("0) Salir");
      System.out.print("Opción: ");
      String op = sc.nextLine().trim();

      switch (op) {
        case "1" -> {
          System.out.print("Username: ");   String u = sc.nextLine().trim();
          System.out.print("Nombre: ");     String n = sc.nextLine().trim();
          System.out.print("Password: ");   String p = sc.nextLine();
          System.out.print("Rol (Doctor|Paciente): "); String r = sc.nextLine().trim();
          if (!r.equals("Doctor") && !r.equals("Paciente")) { System.out.println("Rol inválido."); break; }
          try {
            int id = udao.crearUsuario(u, n, p, r);
            System.out.println("✔ Usuario creado id=" + id);
          } catch (IllegalStateException e) { System.out.println("✖ " + e.getMessage()); }
        }
        case "2" -> {
          System.out.print("Username: "); String u = sc.nextLine().trim();
          System.out.print("Password: "); String p = sc.nextLine();
          try {
            var r = auth.login(u, p);
            usuarioActual = r.user();
            sesionId = r.sesionId();
            System.out.println("✔ Login OK: " + usuarioActual);
          } catch (IllegalArgumentException e) {
            System.out.println("✖ " + e.getMessage());
          }
        }
        case "0" -> { System.out.println("Adiós!"); return; }
        default -> System.out.println("Opción inválida.");
      }
    } else {
      System.out.println("Usuario: " + usuarioActual.username + " (" + usuarioActual.rol + ")");
      System.out.println("1) Ver agenda por doctor y fecha");
      System.out.println("2) Crear cita (por códigos PAC/DOC)");
      System.out.println("3) Cambiar estado de una cita");
      System.out.println("4) Ver últimas sesiones mías");
      System.out.println("9) Cerrar sesión");
      System.out.println("0) Salir");
      System.out.print("Opción: ");
      String op = sc.nextLine().trim();

      switch (op) {
case "1" -> { // Ver agenda por doctor/fecha (lista + Y/M/D)
  String doc = elegirDoctorCodigo(sc);
  if (doc == null) break;
  var fecha = leerFechaYMD(sc);
  var items = citaSrv.agendaDeDoctor(doc, fecha);
  if (items.isEmpty()) System.out.println("(sin citas)");
  else items.forEach(i -> System.out.println(
      i.hora + " | " + i.paciente + " | " + i.estado +
      (i.observacion == null || i.observacion.isBlank() ? "" : " | " + i.observacion)));
}

case "2" -> { // Crear cita (doctor por lista + Y/M/D)
  System.out.print("Código paciente (ej. PAC-001): ");
  String pac = sc.nextLine().trim();

  String doc = elegirDoctorCodigo(sc);
  if (doc == null) break;

  var fecha = leerFechaYMD(sc);
  var hora  = leerHora(sc, "Hora (HH:mm): ");
  String estado = elegirEstado(sc);
  System.out.print("Observación: ");
  String obs = sc.nextLine();

  try {
    int id = citaSrv.crearPorCodigos(pac, doc, fecha, hora, estado, obs);
    System.out.println("✔ Cita creada id=" + id);
  } catch (IllegalStateException | IllegalArgumentException e) {
    System.out.println("✖ " + e.getMessage());
  }
}

        case "3" -> {
          System.out.print("Id de la cita: ");
          int id = Integer.parseInt(sc.nextLine().trim());
          String estado = elegirEstado(sc);
          try {
            citaSrv.actualizarEstado(id, estado);
            System.out.println("✔ Cita " + id + " -> " + estado);
          } catch (Exception e) {
            System.out.println("✖ " + e.getMessage());
          }
        }
        case "4" -> {
          var sdao = new SesionDaoJdbc();
          sdao.ultimasDelUsuario(usuarioActual.id, 10).forEach(System.out::println);
        }
        case "9" -> {
          auth.logout(sesionId);
          System.out.println("✔ Sesión cerrada");
          usuarioActual = null; sesionId = null;
        }
        case "0" -> { System.out.println("Adiós!"); return; }
        default -> System.out.println("Opción inválida.");
      }
    }
  }
}

// ---------- helpers ----------
private static java.time.LocalDate leerFecha(java.util.Scanner sc, String prompt) {
  while (true) {
    System.out.print(prompt);
    String s = sc.nextLine().trim();
    try { return java.time.LocalDate.parse(s); }
    catch (Exception e) { System.out.println("Formato inválido, usa yyyy-MM-dd"); }
  }
}

private static java.time.LocalTime leerHora(java.util.Scanner sc, String prompt) {
  while (true) {
    System.out.print(prompt);
    String s = sc.nextLine().trim() + ":00"; // completa segundos
    try { return java.time.LocalTime.parse(s); }
    catch (Exception e) { System.out.println("Formato inválido, usa HH:mm"); }
  }
}

private static String elegirEstado(java.util.Scanner sc) {
  System.out.println("Estado: 1) Pendiente  2) Confirmada  3) Cancelada  4) Atendida");
  while (true) {
    System.out.print("Elige (1-4): ");
    String s = sc.nextLine().trim();
    switch (s) {
      case "1": return "Pendiente";
      case "2": return "Confirmada";
      case "3": return "Cancelada";
      case "4": return "Atendida";
      default:  System.out.println("Opción inválida.");
    }
  }
}

private static String elegirDoctorCodigo(java.util.Scanner sc) {
  var ddao = new DoctorDaoJdbc();
  var lista = ddao.listarTodos();
  if (lista.isEmpty()) {
    System.out.println("No hay doctores registrados.");
    return null;
  }
  System.out.println("Elige un doctor:");
  for (int i = 0; i < lista.size(); i++) {
    var d = lista.get(i);
    System.out.printf("%d) %s - %s%s%n", i, d.identificacion, d.nombre,
        (d.especialidad == null || d.especialidad.isBlank()) ? "" : " (" + d.especialidad + ")");
  }
  int idx = leerEnteroEnRango(sc, "Opción [0-" + (lista.size()-1) + "]: ", 0, lista.size()-1);
  return lista.get(idx).identificacion; // devolvemos el código (DOC-001)
}

private static java.time.LocalDate leerFechaYMD(java.util.Scanner sc) {
  int anio  = leerEnteroEnRango(sc, "Año (yyyy): ", 1900, 2100);
  int mes   = leerEnteroEnRango(sc, "Mes (1-12): ", 1, 12);
  while (true) {
    int dia = leerEnteroEnRango(sc, "Día (1-31): ", 1, 31);
    try { return java.time.LocalDate.of(anio, mes, dia); }
    catch (Exception e) { System.out.println("Fecha inválida, intenta de nuevo."); }
  }
}

private static int leerEnteroEnRango(java.util.Scanner sc, String prompt, int min, int max) {
  while (true) {
    System.out.print(prompt);
    String s = sc.nextLine().trim();
    try {
      int v = Integer.parseInt(s);
      if (v < min || v > max) { System.out.println("Fuera de rango."); continue; }
      return v;
    } catch (NumberFormatException e) {
      System.out.println("Número inválido.");
    }
  }
}


*/
    
}
