-- Tokens de integração (C-5): SOMENTE hash SHA-256 + prefixo armazenados.
-- A chave em claro é exibida 1x na criação e nunca persistida.
CREATE TABLE IF NOT EXISTS token_integracao (
    id         BIGSERIAL   NOT NULL,
    nome       VARCHAR(128) NOT NULL,
    prefixo    VARCHAR(32)  NOT NULL,
    hash       VARCHAR(64)  NOT NULL UNIQUE,
    criado_em  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    ultimo_uso TIMESTAMPTZ  NULL,
    revogado   BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_token_integracao PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS ix_token_integracao_hash ON token_integracao (hash);
