CREATE TABLE solicitacao_compra (
    id_solicitacao BIGSERIAL PRIMARY KEY,
    id_item_estoque INTEGER NOT NULL,
    quantidade NUMERIC(12, 2) NOT NULL CHECK (quantidade > 0),
    valor_estimado NUMERIC(14, 2),
    status VARCHAR(16) NOT NULL
        CHECK (status IN ('REGISTRADA', 'VISUALIZADA', 'CONCLUIDA')),
    data_solicitacao DATE NOT NULL
);
