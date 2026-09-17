-- Acúmulo de horas validadas por encomenda (custeio por ordem)
CREATE TABLE horas_encomenda (
    id_horas BIGSERIAL PRIMARY KEY,
    id_encomenda BIGINT NOT NULL,
    id_funcionario BIGINT NOT NULL,
    nivel_acesso INTEGER,
    horas NUMERIC(12, 2) NOT NULL,
    data_registro DATE NOT NULL
);

CREATE INDEX idx_horas_encomenda_id_encomenda ON horas_encomenda (id_encomenda);