CREATE TABLE advertencia_membro (
    id_advertencia     BIGSERIAL PRIMARY KEY,
    id_funcionario     BIGINT NOT NULL,
    id_inspecao        BIGINT,
    data               DATE NOT NULL,
    motivo             VARCHAR(500) NOT NULL,
    tipo               VARCHAR(20) NOT NULL,
    contador           INTEGER NOT NULL,
    id_admin_registrou BIGINT,
    CONSTRAINT fk_advertencia_inspecao FOREIGN KEY (id_inspecao) REFERENCES inspecao_5s (id_inspecao)
);

CREATE INDEX idx_advertencia_funcionario ON advertencia_membro (id_funcionario);