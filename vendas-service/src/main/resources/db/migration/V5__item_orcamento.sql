CREATE TABLE item_orcamento (
    id_item_orcamento SERIAL PRIMARY KEY,
    id_orcamento INTEGER NOT NULL REFERENCES orcamento (id_orcamento) ON DELETE CASCADE,
    descricao VARCHAR(500) NOT NULL,
    quantidade DECIMAL(12, 2) NOT NULL,
    valor_unitario DECIMAL(12, 2) NOT NULL,
    material_tipo VARCHAR(64),
    material_quantidade DECIMAL(12, 3),
    material_unidade VARCHAR(16),
    horas DECIMAL(10, 2),
    compra BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_item_orcamento_orcamento ON item_orcamento (id_orcamento);
