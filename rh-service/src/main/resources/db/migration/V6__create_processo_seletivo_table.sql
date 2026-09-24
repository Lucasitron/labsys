-- Processo seletivo dos candidatos.
CREATE TABLE IF NOT EXISTS processo_seletivo (
    id                BIGSERIAL PRIMARY KEY,
    id_candidato      BIGINT      NOT NULL REFERENCES pessoa (id),
    id_tutor          BIGINT      NOT NULL REFERENCES funcionario (id),
    status_processo   VARCHAR(32) NOT NULL CHECK (status_processo IN ('INSCRITO', 'EM_TRIAGEM', 'ENTREVISTA', 'APROVADO', 'REPROVADO')),
    data_inscricao    DATE        NOT NULL,
    resultado_final   VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS ix_processo_candidato ON processo_seletivo (id_candidato);