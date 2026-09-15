CREATE TABLE orcamento (
    id BIGSERIAL PRIMARY KEY,
    id_cliente BIGINT NOT NULL REFERENCES cliente(id),
    data_criacao DATE NOT NULL DEFAULT CURRENT_DATE,
    validade DATE NOT NULL,
    valor_total NUMERIC(14,2) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    observacoes VARCHAR(500)
);
CREATE INDEX idx_orcamento_cliente ON orcamento (id_cliente);
CREATE INDEX idx_orcamento_status ON orcamento (status);