-- Apontamento de horas dedicadas a encomendas ou projetos.
CREATE TABLE IF NOT EXISTS apontamento_horas (
    id                   BIGSERIAL PRIMARY KEY,
    id_funcionario       BIGINT       NOT NULL REFERENCES funcionario (id),
    tipo                 VARCHAR(32)  NOT NULL CHECK (tipo IN ('ENCOMENDA', 'PROJETO')),
    id_referencia        BIGINT       NOT NULL,
    data                 DATE         NOT NULL,
    horas_trabalhadas    NUMERIC(5,2) NOT NULL,
    descricao_atividade  TEXT,
    status               VARCHAR(32)  NOT NULL CHECK (status IN ('PENDENTE', 'VALIDADO', 'REJEITADO')),
    id_admin_validador   BIGINT       REFERENCES funcionario (id),
    data_validacao       TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS ix_apontamento_funcionario ON apontamento_horas (id_funcionario, data);