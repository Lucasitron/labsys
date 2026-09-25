CREATE TABLE setor_responsavel (
    id_responsavel BIGSERIAL PRIMARY KEY,
    id_setor       BIGINT NOT NULL,
    id_funcionario BIGINT NOT NULL,
    data_inicio    DATE NOT NULL,
    data_fim       DATE,
    ativo          BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_responsavel_setor FOREIGN KEY (id_setor) REFERENCES setor (id_setor)
);

CREATE INDEX idx_responsavel_setor ON setor_responsavel (id_setor);