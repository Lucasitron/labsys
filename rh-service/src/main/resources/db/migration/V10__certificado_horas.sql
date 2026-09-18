-- Certificado de horas: consolidação de apontamentos e extrato mensal.
ALTER TABLE apontamento_horas
    ADD COLUMN IF NOT EXISTS consolidado BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS solicitacao_certificado (
    id                  BIGSERIAL PRIMARY KEY,
    id_funcionario      BIGINT       NOT NULL REFERENCES funcionario (id),
    tipo_certificado    VARCHAR(32)  NOT NULL CHECK (tipo_certificado IN ('EXTENSAO', 'COMPLEMENTAR', 'ESTAGIO')),
    data_solicitacao    TIMESTAMPTZ  NOT NULL,
    horas_solicitadas   NUMERIC(7,2) NOT NULL,
    status              VARCHAR(32)  NOT NULL CHECK (status IN ('PENDENTE', 'APROVADO', 'REJEITADO')),
    id_admin_aprovador  BIGINT       REFERENCES funcionario (id),
    data_decisao        TIMESTAMPTZ,
    observacao          TEXT
);

CREATE INDEX IF NOT EXISTS ix_solicitacao_funcionario ON solicitacao_certificado (id_funcionario);

CREATE TABLE IF NOT EXISTS certificado_emitido (
    id                  BIGSERIAL PRIMARY KEY,
    id_solicitacao      BIGINT       NOT NULL UNIQUE REFERENCES solicitacao_certificado (id),
    id_funcionario      BIGINT       NOT NULL REFERENCES funcionario (id),
    tipo_certificado    VARCHAR(32)  NOT NULL CHECK (tipo_certificado IN ('EXTENSAO', 'COMPLEMENTAR', 'ESTAGIO')),
    horas_certificadas  NUMERIC(7,2) NOT NULL,
    data_emissao        TIMESTAMPTZ  NOT NULL,
    codigo_verificacao  UUID         NOT NULL UNIQUE
);

CREATE INDEX IF NOT EXISTS ix_certificado_funcionario ON certificado_emitido (id_funcionario);

CREATE TABLE IF NOT EXISTS hora_consolidada (
    id                  BIGSERIAL PRIMARY KEY,
    id_certificado      BIGINT       NOT NULL REFERENCES certificado_emitido (id),
    id_apontamento      BIGINT       NOT NULL REFERENCES apontamento_horas (id),
    horas               NUMERIC(5,2) NOT NULL,
    data_consolidacao   TIMESTAMPTZ  NOT NULL
);

CREATE INDEX IF NOT EXISTS ix_hora_consolidada_certificado ON hora_consolidada (id_certificado);
