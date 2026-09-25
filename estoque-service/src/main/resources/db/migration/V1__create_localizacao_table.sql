-- Tabela de localizações físicas dos itens.
CREATE TABLE IF NOT EXISTS localizacao (
    id_localizacao BIGSERIAL PRIMARY KEY,
    armario        VARCHAR(255),
    prateleira     VARCHAR(255),
    caixa          VARCHAR(255),
    descricao      VARCHAR(255)
);