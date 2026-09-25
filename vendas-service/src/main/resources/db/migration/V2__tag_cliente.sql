CREATE TABLE tag_cliente (
    id_tag SERIAL PRIMARY KEY,
    nome VARCHAR(64) NOT NULL UNIQUE,
    cor VARCHAR(16)
);
