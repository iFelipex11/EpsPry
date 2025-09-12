-- ====== Crear/usar la base ======
CREATE DATABASE IF NOT EXISTS clinica
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE clinica;

-- (ATENCIÓN) Limpieza total para rehacer el esquema
DROP TABLE IF EXISTS Historia_Nota, Historia_Clinica, Cita,
                     Paciente, Doctor, Sesion, Usuario;

-- ====== Tablas ======
CREATE TABLE Usuario (
  id        INT PRIMARY KEY AUTO_INCREMENT,
  username  VARCHAR(100) NOT NULL UNIQUE,
  nombre    VARCHAR(150) NOT NULL,
  password  VARCHAR(255) NOT NULL,
  rol       ENUM('Doctor','Paciente') NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE Sesion (
  id               INT PRIMARY KEY AUTO_INCREMENT,
  usuario_id       INT NOT NULL,
  instante_inicio  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  estado           ENUM('Activa','Cerrada') NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE Doctor (
  id               INT PRIMARY KEY AUTO_INCREMENT,
  identificacion   VARCHAR(50) NOT NULL UNIQUE,
  nombre           VARCHAR(150) NOT NULL,
  correo           VARCHAR(150),
  telefono         VARCHAR(50),
  genero           VARCHAR(20),
  especialidad     VARCHAR(120),
  sede             VARCHAR(120),
  horario          VARCHAR(120),
  -- Enlace opcional al usuario (si el doctor inicia sesión)
  usuario_id       INT UNIQUE NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE Paciente (
  id               INT PRIMARY KEY AUTO_INCREMENT,
  identificacion   VARCHAR(50) NOT NULL UNIQUE,
  nombre           VARCHAR(150) NOT NULL,
  correo           VARCHAR(150),
  telefono         VARCHAR(50),
  genero           VARCHAR(20),
  direccion        TEXT,
  fecha_nacimiento DATE,
  -- Enlace al usuario que inicia sesión como paciente
  usuario_id       INT UNIQUE NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE Cita (
  id             INT PRIMARY KEY AUTO_INCREMENT,
  paciente_id    INT NOT NULL,
  doctor_id      INT NOT NULL,
  fecha          DATE NOT NULL,
  hora           TIME NOT NULL,
  estado         ENUM('Pendiente','Confirmada','Cancelada','Atendida') NOT NULL,
  observacion    TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE Historia_Clinica (
  id            INT PRIMARY KEY AUTO_INCREMENT,
  paciente_id   INT NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE Historia_Nota (
  id             INT PRIMARY KEY AUTO_INCREMENT,
  historia_id    INT NOT NULL,
  texto          TEXT NOT NULL,
  creada_en      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== Índices ======
CREATE UNIQUE INDEX uq_cita_doctor_fecha_hora ON Cita (doctor_id, fecha, hora);
CREATE INDEX ix_cita_paciente ON Cita (paciente_id);
CREATE INDEX ix_cita_doctor   ON Cita (doctor_id);

CREATE INDEX ix_historia_paciente ON Historia_Clinica(paciente_id);
CREATE INDEX ix_historia_nota_historia ON Historia_Nota(historia_id);

-- Índices para enlace Usuario ↔ Paciente/Doctor
CREATE INDEX ix_paciente_usuario ON Paciente(usuario_id);
CREATE INDEX ix_doctor_usuario   ON Doctor(usuario_id);

-- ====== Claves Foráneas ======
ALTER TABLE Sesion
  ADD CONSTRAINT fk_sesion_usuario
  FOREIGN KEY (usuario_id) REFERENCES Usuario(id)
  ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE Doctor
  ADD CONSTRAINT fk_doctor_usuario
  FOREIGN KEY (usuario_id) REFERENCES Usuario(id)
  ON DELETE SET NULL ON UPDATE CASCADE;

ALTER TABLE Paciente
  ADD CONSTRAINT fk_paciente_usuario
  FOREIGN KEY (usuario_id) REFERENCES Usuario(id)
  ON DELETE SET NULL ON UPDATE CASCADE;

ALTER TABLE Cita
  ADD CONSTRAINT fk_cita_doctor
  FOREIGN KEY (doctor_id) REFERENCES Doctor(id)
  ON DELETE RESTRICT ON UPDATE CASCADE,
  ADD CONSTRAINT fk_cita_paciente
  FOREIGN KEY (paciente_id) REFERENCES Paciente(id)
  ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE Historia_Clinica
  ADD CONSTRAINT fk_historia_paciente
  FOREIGN KEY (paciente_id) REFERENCES Paciente(id)
  ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE Historia_Nota
  ADD CONSTRAINT fk_historia_nota_historia
  FOREIGN KEY (historia_id) REFERENCES Historia_Clinica(id)
  ON DELETE CASCADE ON UPDATE CASCADE;

-- ====== Trigger de auto-creación de Doctor/Paciente al crear Usuario ======
DROP TRIGGER IF EXISTS trg_usuario_autolink;
DELIMITER $$
CREATE TRIGGER trg_usuario_autolink
AFTER INSERT ON Usuario
FOR EACH ROW
BEGIN
  IF NEW.rol = 'Paciente' THEN
    INSERT INTO Paciente(identificacion, nombre, usuario_id)
    VALUES (CONCAT('PAC-', NEW.id), NEW.nombre, NEW.id);
  ELSEIF NEW.rol = 'Doctor' THEN
    INSERT INTO Doctor(identificacion, nombre, usuario_id)
    VALUES (CONCAT('DOC-', NEW.id), NEW.nombre, NEW.id);
  END IF;
END$$
DELIMITER ;
