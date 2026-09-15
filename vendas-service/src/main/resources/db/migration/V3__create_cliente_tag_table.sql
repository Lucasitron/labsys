CREATE TABLE cliente_tag (
    id BIGSERIAL PRIMARY KEY,
    id_cliente BIGINT NOT NULL REFERENCES cliente(id) ON DELETE CASCADE,
    id_tag BIGINT NOT NULL REFERENCES tag_cliente(id) ON DELETE CASCADE,
    UNIQUE (id_cliente, id_tag)
);
CREATE INDEX idx_cliente_tag_cliente ON cliente_tag (id_cliente);
CREATE INDEX idx_cliente_tag_tag ON cliente_tag (id_tag);