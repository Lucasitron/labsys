CREATE TABLE registro_marketplace (
    id_registro SERIAL PRIMARY KEY,
    id_encomenda INTEGER NOT NULL REFERENCES encomenda (id_encomenda),
    plataforma VARCHAR(64) NOT NULL,
    codigo_externo VARCHAR(64) NOT NULL,
    data_venda DATE NOT NULL,
    valor_taxa DECIMAL(12, 2) NOT NULL,
    CONSTRAINT uq_marketplace_plataforma_codigo UNIQUE (plataforma, codigo_externo)
);
CREATE INDEX idx_marketplace_encomenda ON registro_marketplace (id_encomenda);
CREATE INDEX idx_marketplace_plataforma ON registro_marketplace (plataforma);
