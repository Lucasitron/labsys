-- Tabela de saídas de estoque (consumo, perda, ajuste, empréstimo).
CREATE TABLE IF NOT EXISTS saida_estoque (
    id_saida     BIGSERIAL PRIMARY KEY,
    id_item      BIGINT NOT NULL,
    quantidade   NUMERIC(12, 2) NOT NULL,
    tipo_saida   VARCHAR(32) NOT NULL,
    id_referencia BIGINT,
    data_saida   TIMESTAMP NOT NULL,
    observacao   VARCHAR(255),
    CONSTRAINT fk_saida_item FOREIGN KEY (id_item) REFERENCES item (id_item),
    CONSTRAINT ck_saida_tipo CHECK (tipo_saida IN ('CONSUMO', 'PERDA', 'AJUSTE', 'EMPRESTIMO'))
);

CREATE INDEX IF NOT EXISTS idx_saida_item ON saida_estoque (id_item);