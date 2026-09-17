-- Categoria financeira (receita/despesa)
CREATE TABLE categoria_financeira (
    id_categoria BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('RECEITA', 'DESPESA')),
    descricao VARCHAR(255)
);