-- Tabela de registro de acesso físico (ponto eletrônico / controle de acesso).
CREATE TABLE IF NOT EXISTS access_log (
    id          BIGSERIAL PRIMARY KEY,
    id_user     BIGINT,
    uuid_rfid   VARCHAR(255) NOT NULL,
    timestamp   TIMESTAMPTZ  NOT NULL,
    type        VARCHAR(32)  NOT NULL
);

CREATE INDEX IF NOT EXISTS ix_access_log_uuid ON access_log (uuid_rfid, timestamp DESC);