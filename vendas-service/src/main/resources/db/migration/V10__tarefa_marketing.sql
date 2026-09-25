CREATE TABLE tarefa_marketing (
    id_tarefa SERIAL PRIMARY KEY,
    titulo VARCHAR(255) NOT NULL,
    descricao VARCHAR(2000),
    id_responsavel BIGINT NOT NULL,
    data_inicio DATE,
    data_fim DATE,
    status VARCHAR(16) NOT NULL CHECK (status IN ('Pendente', 'Em Andamento', 'Concluída')),
    prioridade VARCHAR(16) NOT NULL CHECK (prioridade IN ('Baixa', 'Média', 'Alta')),
    criado_por BIGINT NOT NULL
);
CREATE INDEX idx_tarefa_responsavel ON tarefa_marketing (id_responsavel);
CREATE INDEX idx_tarefa_status ON tarefa_marketing (status);
