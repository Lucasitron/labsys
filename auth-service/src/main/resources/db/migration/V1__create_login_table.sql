-- Tabela de credenciais e identidade do usuário.
CREATE TABLE IF NOT EXISTS login (
    id          BIGSERIAL PRIMARY KEY,
    id_user     BIGINT      NOT NULL,
    uuid        VARCHAR(255) NOT NULL,
    email       VARCHAR(255) NOT NULL,
    nome_usuario VARCHAR(255) NOT NULL,
    senha_hash  VARCHAR(255) NOT NULL,
    setor       VARCHAR(255),
    CONSTRAINT uk_login_uuid         UNIQUE (uuid),
    CONSTRAINT uk_login_email        UNIQUE (email),
    CONSTRAINT uk_login_nome_usuario UNIQUE (nome_usuario)
);