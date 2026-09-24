-- Tabela de pessoas do Fab Lab.
CREATE TABLE IF NOT EXISTS pessoa (
    id              BIGSERIAL PRIMARY KEY,
    nome_completo   VARCHAR(255) NOT NULL,
    matricula       VARCHAR(64)  NOT NULL,
    data_admissao   DATE,
    contato         VARCHAR(255),
    turno           VARCHAR(32),
    status          INTEGER      NOT NULL,
    CONSTRAINT uk_pessoa_matricula UNIQUE (matricula)
);