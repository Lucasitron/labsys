CREATE TABLE registro_marketplace (
    id BIGSERIAL PRIMARY KEY,
    id_encomenda BIGINT NOT NULL REFERENCES encomenda(id) ON DELETE CASCADE,
    plataforma VARCHAR(100) NOT NULL,
    codigo_externo VARCHAR(100),
    data_venda DATE NOT NULL,
    valor_taxa NUMERIC(14,2)
);
CREATE INDEX idx_marketplace_encomenda ON registro_marketplace (id_encomenda);