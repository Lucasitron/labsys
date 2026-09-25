CREATE TABLE categoria_financeira (
    id_categoria BIGSERIAL PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    tipo VARCHAR(16) NOT NULL
        CHECK (tipo IN ('RECEITA', 'DESPESA')),
    descricao VARCHAR(500)
);
