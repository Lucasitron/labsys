-- Tabela de tutores.
CREATE TABLE IF NOT EXISTS tutor (
    id              BIGSERIAL PRIMARY KEY,
    id_funcionario  BIGINT  NOT NULL REFERENCES funcionario (id),
    turno           VARCHAR(32),
    qualificacao    INTEGER CHECK (qualificacao BETWEEN 0 AND 6),
    CONSTRAINT uk_tutor_funcionario UNIQUE (id_funcionario)
);