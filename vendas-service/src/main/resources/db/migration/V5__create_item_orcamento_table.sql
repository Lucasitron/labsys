CREATE TABLE item_orcamento (
    id BIGSERIAL PRIMARY KEY,
    id_orcamento BIGINT NOT NULL REFERENCES orcamento(id) ON DELETE CASCADE,
    descricao VARCHAR(300) NOT NULL,
    quantidade NUMERIC(12,2) NOT NULL,
    valor_unitario NUMERIC(14,2) NOT NULL
);
CREATE INDEX idx_item_orcamento_orcamento ON item_orcamento (id_orcamento);