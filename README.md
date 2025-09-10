# EpsPry

## Sistema de gestion de citas

Este proyecto fue hecho con javafx y mysql para gestionar la informacion de una clinica, permitiendo a doctores y pacientes interactuar en un sistema centralizado con autenticacion, citas medicas y registro clinico

### Principales funciones
- Registro de usuarios segun su rol
- inicio de sesion con autenticacion
- recuperacion de contraseña de manera simple

### Modulo doctor
- El doctor tiene una agenda de citas programadas dependiendo del dia

- Puede gestionar las citas en estos 4 estados (Pendiente, Confirmada, Cancelada, Atendida)

- Tiene acceso a la historia clinica de cada paciente

### Modulo Paciente

- Panel de control para revisar citas pasadas y futuras.

- Solicitud de nuevas citas con doctores disponibles.

- Cancelación de citas en estado pendiente o confirmada.

### Historias Clinicas

- Cada paciente cuenta con una historia clínica única.

- Registro cronológico de notas médicas.

### Persistencia con MySQL
- Base de datos normalizada con entidades:
- Usuario
- Doctor
- Paciente
- Cita
- Historia_Clinica
- Historia_Nota
- Sesion.

### Tecnologías utilizadas

- Java 17+

- JavaFX (UI y controladores)

- MySQL 8 (persistencia de datos)

- JDBC (acceso a base de datos)

- Git/GitHub para control de versiones
