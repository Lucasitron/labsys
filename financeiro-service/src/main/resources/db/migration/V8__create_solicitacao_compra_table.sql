-- Solicitações de compra (fluxo informativo para o Estoque)
CREATE TABLE solicitacao_compra (
    id_solicitacao BIGSERIAL PRIMARY KEY,
    id_item_estoque BIGINT NOT NULL,
    quantidade NUMERIC(12, 2) NOT NULL,
    valor_estimado NUMERIC(14, 2) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('REGISTRADA', 'VISUALIZADA', 'CONCLUIDA')),
    data_solicitacao DATE NOT NULL
);