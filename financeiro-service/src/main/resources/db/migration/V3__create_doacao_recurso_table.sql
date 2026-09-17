-- Doações e recursos de projetos universitários
CREATE TABLE doacao_recurso (
    id_doacao BIGSERIAL PRIMARY KEY,
    tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('DOACAO', 'PROJETO')),
    origem VARCHAR(150) NOT NULL,
    valor NUMERIC(14, 2) NOT NULL,
    data_recebimento DATE NOT NULL,
    id_projeto_associado BIGINT
);