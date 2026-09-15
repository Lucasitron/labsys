CREATE TABLE interacao_cliente (
    id BIGSERIAL PRIMARY KEY,
    id_cliente BIGINT NOT NULL REFERENCES cliente(id) ON DELETE CASCADE,
    data_interacao TIMESTAMP NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    descricao VARCHAR(500),
    id_usuario BIGINT
);
CREATE INDEX idx_interacao_cliente ON interacao_cliente (id_cliente);