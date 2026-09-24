CREATE TABLE setor_sinalizacao (
    id_sinalizacao BIGSERIAL PRIMARY KEY,
    id_setor       BIGINT NOT NULL,
    texto          VARCHAR(255) NOT NULL,
    CONSTRAINT fk_sinalizacao_setor FOREIGN KEY (id_setor) REFERENCES setor (id_setor)
);

CREATE INDEX idx_sinalizacao_setor ON setor_sinalizacao (id_setor);