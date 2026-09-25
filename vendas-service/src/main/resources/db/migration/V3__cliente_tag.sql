CREATE TABLE cliente_tag (
    id_cliente_tag SERIAL PRIMARY KEY,
    id_cliente INTEGER NOT NULL REFERENCES cliente (id_cliente),
    id_tag INTEGER NOT NULL REFERENCES tag_cliente (id_tag),
    CONSTRAINT uq_cliente_tag UNIQUE (id_cliente, id_tag)
);
CREATE INDEX idx_cliente_tag_cliente ON cliente_tag (id_cliente);
CREATE INDEX idx_cliente_tag_tag ON cliente_tag (id_tag);
