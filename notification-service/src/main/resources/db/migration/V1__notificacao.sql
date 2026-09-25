-- Notificações endereçadas aos usuários do Auth Service.
CREATE TABLE IF NOT EXISTS notificacao (
    id          BIGSERIAL PRIMARY KEY,
    id_usuario  BIGINT       NOT NULL,
    titulo      VARCHAR(160) NOT NULL,
    mensagem    TEXT,
    lida        BOOLEAN      NOT NULL DEFAULT FALSE,
    criada_em   TIMESTAMPTZ  NOT NULL
);

CREATE INDEX IF NOT EXISTS ix_notificacao_usuario ON notificacao (id_usuario, lida);