CREATE TABLE cliente (
    id BIGSERIAL PRIMARY KEY,
    tipo_pessoa VARCHAR(2) NOT NULL,
    nome_razao_social VARCHAR(200) NOT NULL,
    cpf_cnpj VARCHAR(20) NOT NULL UNIQUE,
    email VARCHAR(150),
    telefone VARCHAR(30),
    endereco VARCHAR(300),
    data_cadastro DATE NOT NULL DEFAULT CURRENT_DATE
);
CREATE INDEX idx_cliente_nome ON cliente (nome_razao_social);
CREATE INDEX idx_cliente_cpf_cnpj ON cliente (cpf_cnpj);