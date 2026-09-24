CREATE TABLE setor (
    id_setor          BIGSERIAL PRIMARY KEY,
    numero            INTEGER NOT NULL,
    nome              VARCHAR(150) NOT NULL,
    descricao         VARCHAR(1000),
    observacoes       VARCHAR(1000),
    foto_correto_url  VARCHAR(500),
    foto_incorreto_url VARCHAR(500),
    ativo             BOOLEAN NOT NULL DEFAULT TRUE
);