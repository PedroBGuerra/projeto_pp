CREATE DATABASE crud_db;

USE crud_db;

CREATE TABLE estacionamento (
    id INT AUTO_INCREMENT PRIMARY KEY,
    marca VARCHAR(255),
    modelo VARCHAR(255),
    placa VARCHAR(20),
    entrada DATETIME,
    saida DATETIME
);

CREATE TABLE precos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    max_hours INT NULL,
    base_price DOUBLE NOT NULL,
    extra_per_hour DOUBLE NULL
);

INSERT INTO precos (max_hours, base_price, extra_per_hour) VALUES (1, 15, NULL);
INSERT INTO precos (max_hours, base_price, extra_per_hour) VALUES (2, 25, NULL);
INSERT INTO precos (max_hours, base_price, extra_per_hour) VALUES (NULL, 30, 5);

