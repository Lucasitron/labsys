CREATE TABLE orcamento (
    id_orcamento SERIAL PRIMARY KEY,
    id_cliente INTEGER NOT NULL REFERENCES cliente (id_cliente),
    data_criacao DATE NOT NULL,
    validade DATE,
    valor_total DECIMAL(12, 2) NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('Pendente', 'Aprovado', 'Recusado', 'Ajuste')),
    observacoes VARCHAR(1000),
    criado_por BIGINT NOT NULL
);
CREATE INDEX idx_orcamento_cliente ON orcamento (id_cliente);
CREATE INDEX idx_orcamento_status ON orcamento (status);
