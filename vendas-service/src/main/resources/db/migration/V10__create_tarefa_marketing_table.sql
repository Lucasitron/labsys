CREATE TABLE tarefa_marketing (
    id BIGSERIAL PRIMARY KEY,
    titulo VARCHAR(200) NOT NULL,
    descricao VARCHAR(500),
    id_responsavel BIGINT NOT NULL,
    data_inicio DATE,
    data_fim DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    prioridade VARCHAR(20) NOT NULL DEFAULT 'MEDIA'
);
CREATE INDEX idx_tarefa_responsavel ON tarefa_marketing (id_responsavel);
CREATE INDEX idx_tarefa_status ON tarefa_marketing (status);