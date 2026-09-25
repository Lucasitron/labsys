-- Tabela de fornecedores.
CREATE TABLE IF NOT EXISTS fornecedor (
    id_fornecedor BIGSERIAL PRIMARY KEY,
    nome          VARCHAR(255) NOT NULL,
    contato       VARCHAR(255),
    cnpj          VARCHAR(32)
);