-- ====== Base y limpieza ======
CREATE DATABASE IF NOT EXISTS clinica
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE clinica;

-- Limpieza ordenada
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS Historia_Nota, Historia_Clinica, Cita,
                     Paciente, Doctor, Sesion, Usuario;
SET FOREIGN_KEY_CHECKS = 1;

-- ====== Tablas ======
CREATE TABLE Usuario (
  id        INT PRIMARY KEY AUTO_INCREMENT,
  username  VARCHAR(100) NOT NULL UNIQUE,
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
  cedula           VARCHAR(50) UNIQUE,
  nombre1          VARCHAR(150) NOT NULL,
  nombre2          VARCHAR(150),
  apellido1        VARCHAR(150) NOT NULL,
  apellido2        VARCHAR(150),
  correo           VARCHAR(150),
  telefono         VARCHAR(50),
  genero           VARCHAR(20),
  especialidad     VARCHAR(120),
  sede             VARCHAR(120),
  horario          VARCHAR(120),
  usuario_id       INT UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE Paciente (
  id               INT PRIMARY KEY AUTO_INCREMENT,
  identificacion   VARCHAR(50) NOT NULL UNIQUE,
  cedula           VARCHAR(50) UNIQUE,
  nombre1          VARCHAR(150) NOT NULL,
  nombre2          VARCHAR(150),
  apellido1        VARCHAR(150) NOT NULL,
  apellido2        VARCHAR(150),
  correo           VARCHAR(150),
  telefono         VARCHAR(50),
  genero           VARCHAR(20),
  direccion        TEXT,
  fecha_nacimiento DATE,
  usuario_id       INT UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE Cita (
  id             INT PRIMARY KEY AUTO_INCREMENT,
  paciente_id    INT NOT NULL,
  doctor_id      INT NOT NULL,
  fecha          DATE NOT NULL,
  hora           TIME NOT NULL,
  estado         ENUM('Pendiente','Confirmada','Cancelada','Atendida') NOT NULL DEFAULT 'Pendiente',
  observacion    TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE Historia_Clinica (
  id            INT PRIMARY KEY AUTO_INCREMENT,
  paciente_id   INT NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE Historia_Nota (
  id               INT PRIMARY KEY AUTO_INCREMENT,
  historia_id      INT NOT NULL,
  texto            TEXT,
  alergias         VARCHAR(150),
  medicamentos     VARCHAR(150),
  motivo_consulta  VARCHAR(150),
  recomendaciones  VARCHAR(150),
  creada_en        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====== Índices ======
CREATE UNIQUE INDEX uq_cita_doctor_fecha_hora ON Cita (doctor_id, fecha, hora);
CREATE INDEX ix_cita_paciente ON Cita (paciente_id);
CREATE INDEX ix_cita_doctor   ON Cita (doctor_id);

CREATE INDEX ix_historia_paciente ON Historia_Clinica(paciente_id);
CREATE INDEX ix_historia_nota_historia ON Historia_Nota(historia_id);

CREATE INDEX ix_paciente_usuario ON Paciente(usuario_id);
CREATE INDEX ix_doctor_usuario   ON Doctor(usuario_id);

-- ====== Claves Foráneas (corregidas) ======
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

-- ====== Triggers ======

-- 1) Auto-vincular Usuario -> (Paciente|Doctor)
DROP TRIGGER IF EXISTS trg_usuario_autolink;
DELIMITER $$
CREATE TRIGGER trg_usuario_autolink
AFTER INSERT ON Usuario
FOR EACH ROW
BEGIN
  DECLARE auto_ident VARCHAR(50);
  DECLARE auto_ced   VARCHAR(50);

  SET auto_ident = (CASE WHEN NEW.rol='Paciente' THEN CONCAT('PAC-', NEW.id)
                         ELSE CONCAT('DOC-', NEW.id) END);
  SET auto_ced   = CONCAT('AUTO-', NEW.id);

  IF NEW.rol = 'Paciente' THEN
    INSERT INTO Paciente(
      identificacion, cedula,
      nombre1, nombre2, apellido1, apellido2,
      correo, telefono, genero, direccion, fecha_nacimiento,
      usuario_id
    ) VALUES (
      auto_ident, auto_ced,
      NEW.username, NULL, 'Pendiente', NULL,
      NULL, NULL, NULL, NULL, NULL,
      NEW.id
    );
  ELSE
    INSERT INTO Doctor(
      identificacion, cedula,
      nombre1, nombre2, apellido1, apellido2,
      correo, telefono, genero, especialidad, sede, horario,
      usuario_id
    ) VALUES (
      auto_ident, auto_ced,
      NEW.username, NULL, 'Pendiente', NULL,
      NULL, NULL, NULL, NULL, NULL, NULL,
      NEW.id
    );
  END IF;
END$$
DELIMITER ;

-- 2) Auto-crear Historia_Clinica al insertar Paciente
DROP TRIGGER IF EXISTS trg_paciente_historia;
DELIMITER $$
CREATE TRIGGER trg_paciente_historia
AFTER INSERT ON Paciente
FOR EACH ROW
BEGIN
  INSERT INTO Historia_Clinica(paciente_id) VALUES (NEW.id);
END$$
DELIMITER ;

-- 3) Validación adicional de solapamiento de Cita (además del índice único)
DROP TRIGGER IF EXISTS trg_cita_no_overlap_ins;
DELIMITER $$
CREATE TRIGGER trg_cita_no_overlap_ins
BEFORE INSERT ON Cita
FOR EACH ROW
BEGIN
  IF EXISTS(
    SELECT 1 FROM Cita
     WHERE doctor_id = NEW.doctor_id
       AND fecha = NEW.fecha
       AND hora  = NEW.hora
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'El doctor ya tiene una cita en esa fecha y hora.';
  END IF;
END$$
DELIMITER ;

DROP TRIGGER IF EXISTS trg_cita_no_overlap_upd;
DELIMITER $$
CREATE TRIGGER trg_cita_no_overlap_upd
BEFORE UPDATE ON Cita
FOR EACH ROW
BEGIN
  IF EXISTS(
    SELECT 1 FROM Cita
     WHERE doctor_id = NEW.doctor_id
       AND fecha = NEW.fecha
       AND hora  = NEW.hora
       AND id   <> OLD.id
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'El doctor ya tiene una cita en esa fecha y hora.';
  END IF;
END$$
DELIMITER ;

-- 4) Limpieza de datos en Historia_Nota
DROP TRIGGER IF EXISTS trg_historia_nota_normaliza;
DELIMITER $$
CREATE TRIGGER trg_historia_nota_normaliza
BEFORE INSERT ON Historia_Nota
FOR EACH ROW
BEGIN
  IF NEW.motivo_consulta IS NOT NULL THEN
    SET NEW.motivo_consulta = TRIM(LOWER(NEW.motivo_consulta));
  END IF;
END$$
DELIMITER ;
