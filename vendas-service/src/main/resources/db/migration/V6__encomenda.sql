CREATE TABLE encomenda (
    id_encomenda SERIAL PRIMARY KEY,
    versao BIGINT,
    id_orcamento INTEGER REFERENCES orcamento (id_orcamento),
    id_cliente INTEGER NOT NULL REFERENCES cliente (id_cliente),
    data_criacao DATE NOT NULL,
    data_previsao_entrega DATE,
    status_kanban VARCHAR(16) NOT NULL
        CHECK (status_kanban IN ('Fila', 'Produção', 'Acabamento', 'Pronto', 'Entregue')),
    valor_final DECIMAL(12, 2) NOT NULL,
    observacoes VARCHAR(1000),
    criado_por BIGINT NOT NULL,
    encomenda_origem_id INTEGER REFERENCES encomenda (id_encomenda)
);
CREATE INDEX idx_encomenda_status ON encomenda (status_kanban);
CREATE INDEX idx_encomenda_cliente ON encomenda (id_cliente);
CREATE INDEX idx_encomenda_orcamento ON encomenda (id_orcamento);
