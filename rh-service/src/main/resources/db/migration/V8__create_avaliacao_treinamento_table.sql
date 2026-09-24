-- Avaliações dos treinamentos (nota 0-10 e feedback).
CREATE TABLE IF NOT EXISTS avaliacao_treinamento (
    id                BIGSERIAL PRIMARY KEY,
    id_treinamento    BIGINT       NOT NULL REFERENCES treinamento (id),
    id_funcionario    BIGINT       NOT NULL REFERENCES funcionario (id),
    nota              NUMERIC(4,2) NOT NULL CHECK (nota BETWEEN 0 AND 10),
    feedback          TEXT,
    data_avaliacao    DATE
);

CREATE INDEX IF NOT EXISTS ix_avaliacao_treinamento ON avaliacao_treinamento (id_treinamento);