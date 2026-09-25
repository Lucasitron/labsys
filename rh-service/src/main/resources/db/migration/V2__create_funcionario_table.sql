-- Tabela de funcionários (vínculo pessoa -> nível de acesso).
CREATE TABLE IF NOT EXISTS funcionario (
    id              BIGSERIAL PRIMARY KEY,
    id_pessoa       BIGINT      NOT NULL REFERENCES pessoa (id),
    nivel_acesso    INTEGER     NOT NULL CHECK (nivel_acesso BETWEEN 0 AND 4),
    departamento    VARCHAR(255),
    CONSTRAINT uk_funcionario_pessoa UNIQUE (id_pessoa)
);