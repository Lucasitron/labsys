CREATE TABLE tarefa (
    id_tarefa        BIGSERIAL PRIMARY KEY,
    id_projeto       BIGINT NOT NULL,
    titulo           VARCHAR(150) NOT NULL,
    descricao        VARCHAR(1000),
    id_responsavel   BIGINT,
    data_inicio      DATE,
    data_fim_prevista DATE,
    data_conclusao   DATE,
    status           VARCHAR(20) NOT NULL,
    prioridade       VARCHAR(20) NOT NULL,
    CONSTRAINT fk_tarefa_projeto FOREIGN KEY (id_projeto) REFERENCES projeto (id_projeto)
);

CREATE INDEX idx_tarefa_projeto ON tarefa (id_projeto);