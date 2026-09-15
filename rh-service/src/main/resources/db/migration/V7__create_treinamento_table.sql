-- Treinamentos do módulo LMS.
CREATE TABLE IF NOT EXISTS treinamento (
    id              BIGSERIAL PRIMARY KEY,
    titulo          VARCHAR(255) NOT NULL,
    descricao       TEXT,
    url_conteudo    VARCHAR(500),
    id_tutor        BIGINT NOT NULL REFERENCES funcionario (id)
);