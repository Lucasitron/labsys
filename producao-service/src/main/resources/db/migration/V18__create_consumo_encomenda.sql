CREATE TABLE consumo_encomenda (
    id_consumo           BIGSERIAL PRIMARY KEY,
    id_encomenda         BIGINT NOT NULL,
    id_item              BIGINT NOT NULL,
    quantidade_consumida NUMERIC(12, 2) NOT NULL
);

CREATE INDEX idx_consumo_encomenda ON consumo_encomenda (id_encomenda);