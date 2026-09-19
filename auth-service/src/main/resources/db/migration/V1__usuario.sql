-- Usuários e responsabilidades granulares do Auth & Identity Service.
CREATE TABLE IF NOT EXISTS usuario (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(64)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nome_completo VARCHAR(160) NOT NULL,
    email         VARCHAR(160) NOT NULL,
    role          INTEGER      NOT NULL CHECK (role BETWEEN 0 AND 4),
    setor         VARCHAR(64),
    ativo         BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS usuario_responsabilidade (
    id          BIGSERIAL PRIMARY KEY,
    id_usuario  BIGINT      NOT NULL REFERENCES usuario (id),
    modulo      VARCHAR(32) NOT NULL,
    recurso     VARCHAR(64) NOT NULL
);

CREATE INDEX IF NOT EXISTS ix_responsabilidade_usuario ON usuario_responsabilidade (id_usuario);