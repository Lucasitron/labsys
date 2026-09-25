CREATE TABLE cliente (
    id_cliente SERIAL PRIMARY KEY,
    tipo_pessoa VARCHAR(8) NOT NULL CHECK (tipo_pessoa IN ('PF', 'PJ')),
    nome_razao_social VARCHAR(255) NOT NULL,
    cpf_cnpj VARCHAR(20) NOT NULL UNIQUE,
    email VARCHAR(255),
    telefone VARCHAR(32),
    endereco VARCHAR(500),
    data_cadastro DATE NOT NULL,
    criado_por BIGINT NOT NULL
);
CREATE INDEX idx_cliente_nome ON cliente (nome_razao_social);
CREATE INDEX idx_cliente_tipo ON cliente (tipo_pessoa);
