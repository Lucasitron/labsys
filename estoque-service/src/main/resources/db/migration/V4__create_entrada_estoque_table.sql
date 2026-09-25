-- Tabela de entradas de estoque (compras simples).
CREATE TABLE IF NOT EXISTS entrada_estoque (
    id_entrada     BIGSERIAL PRIMARY KEY,
    id_item        BIGINT NOT NULL,
    id_fornecedor  BIGINT NOT NULL,
    quantidade     NUMERIC(12, 2) NOT NULL,
    valor_unitario NUMERIC(12, 2) NOT NULL,
    valor_total    NUMERIC(12, 2) NOT NULL,
    data_entrada   DATE NOT NULL,
    nota_fiscal    VARCHAR(255),
    observacao     VARCHAR(255),
    CONSTRAINT fk_entrada_item FOREIGN KEY (id_item) REFERENCES item (id_item),
    CONSTRAINT fk_entrada_fornecedor FOREIGN KEY (id_fornecedor) REFERENCES fornecedor (id_fornecedor)
);

CREATE INDEX IF NOT EXISTS idx_entrada_item ON entrada_estoque (id_item);
CREATE INDEX IF NOT EXISTS idx_entrada_fornecedor ON entrada_estoque (id_fornecedor);