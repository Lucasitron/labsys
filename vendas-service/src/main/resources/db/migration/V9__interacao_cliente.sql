CREATE TABLE interacao_cliente (
    id_interacao SERIAL PRIMARY KEY,
    id_cliente INTEGER NOT NULL REFERENCES cliente (id_cliente) ON DELETE CASCADE,
    data_interacao TIMESTAMP NOT NULL,
    tipo VARCHAR(16) NOT NULL CHECK (tipo IN ('E-mail', 'Telefone', 'Reunião', 'WhatsApp')),
    descricao VARCHAR(2000) NOT NULL,
    id_usuario BIGINT NOT NULL
);
CREATE INDEX idx_interacao_cliente ON interacao_cliente (id_cliente, data_interacao);
