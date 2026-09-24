CREATE TABLE doacao_recurso (
    id_doacao BIGSERIAL PRIMARY KEY,
    tipo VARCHAR(16) NOT NULL
        CHECK (tipo IN ('DOACAO', 'PROJETO')),
    origem VARCHAR(200) NOT NULL,
    valor NUMERIC(14, 2) NOT NULL CHECK (valor > 0),
    data_recebimento DATE NOT NULL,
    id_projeto_associado INTEGER
);
