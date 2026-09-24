-- Grupos do processo seletivo e avaliação individual do candidato.
CREATE TABLE IF NOT EXISTS grupo_processo_seletivo (
    id              BIGSERIAL PRIMARY KEY,
    nome            VARCHAR(255) NOT NULL,
    id_tutor_lider  BIGINT       NOT NULL REFERENCES funcionario (id),
    etapa           VARCHAR(32)  NOT NULL DEFAULT 'INSCRITO'
        CHECK (etapa IN ('INSCRITO', 'EM_TRIAGEM', 'ENTREVISTA', 'APROVADO', 'REPROVADO'))
);

ALTER TABLE processo_seletivo ADD COLUMN IF NOT EXISTS id_grupo BIGINT
    REFERENCES grupo_processo_seletivo (id);
ALTER TABLE processo_seletivo ADD COLUMN IF NOT EXISTS nota NUMERIC(4,2);
ALTER TABLE processo_seletivo ADD COLUMN IF NOT EXISTS feedback TEXT;

CREATE INDEX IF NOT EXISTS ix_processo_grupo ON processo_seletivo (id_grupo);
