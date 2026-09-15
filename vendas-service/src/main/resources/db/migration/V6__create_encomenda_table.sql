CREATE TABLE encomenda (
    id BIGSERIAL PRIMARY KEY,
    id_orcamento BIGINT REFERENCES orcamento(id),
    id_cliente BIGINT NOT NULL REFERENCES cliente(id),
    data_criacao DATE NOT NULL DEFAULT CURRENT_DATE,
    data_previsao_entrega DATE,
    status_kanban VARCHAR(20) NOT NULL DEFAULT 'FILA',
    valor_final NUMERIC(14,2) NOT NULL DEFAULT 0,
    observacoes VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_encomenda_cliente ON encomenda (id_cliente);
CREATE INDEX idx_encomenda_status ON encomenda (status_kanban);