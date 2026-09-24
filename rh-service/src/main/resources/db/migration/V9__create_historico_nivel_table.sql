-- Histórico de alterações de nível de acesso (auditoria).
CREATE TABLE IF NOT EXISTS historico_nivel (
    id                BIGSERIAL PRIMARY KEY,
    id_funcionario    BIGINT      NOT NULL REFERENCES funcionario (id),
    nivel_antigo      INTEGER     NOT NULL CHECK (nivel_antigo BETWEEN 0 AND 4),
    nivel_novo        INTEGER     NOT NULL CHECK (nivel_novo BETWEEN 0 AND 4),
    id_admin_alterou  BIGINT      NOT NULL REFERENCES funcionario (id),
    data_alteracao    TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS ix_historico_funcionario ON historico_nivel (id_funcionario);