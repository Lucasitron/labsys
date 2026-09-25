CREATE TABLE horas_encomenda (
    id_horas BIGSERIAL PRIMARY KEY,
    id_encomenda INTEGER NOT NULL,
    id_funcionario INTEGER NOT NULL,
    nivel_acesso INTEGER CHECK (nivel_acesso BETWEEN 0 AND 3),
    horas NUMERIC(12, 2) NOT NULL CHECK (horas > 0),
    data_registro DATE NOT NULL,
    CONSTRAINT uq_horas_encomenda UNIQUE (id_encomenda, id_funcionario, data_registro)
);
CREATE INDEX idx_horas_encomenda_id_encomenda ON horas_encomenda (id_encomenda);
