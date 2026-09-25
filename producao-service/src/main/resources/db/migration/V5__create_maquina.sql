CREATE TABLE maquina (
    id_maquina  BIGSERIAL PRIMARY KEY,
    nome        VARCHAR(150) NOT NULL,
    descricao   VARCHAR(1000),
    status      VARCHAR(20) NOT NULL,
    localizacao VARCHAR(150)
);