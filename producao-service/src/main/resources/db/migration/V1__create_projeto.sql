CREATE TABLE projeto (
    id_projeto        BIGSERIAL PRIMARY KEY,
    nome              VARCHAR(150) NOT NULL,
    descricao         VARCHAR(1000),
    data_inicio       DATE NOT NULL,
    data_fim_prevista DATE,
    data_fim_real     DATE,
    status            VARCHAR(20) NOT NULL,
    id_responsavel    BIGINT NOT NULL
);