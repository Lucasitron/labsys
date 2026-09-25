CREATE TABLE inspecao_5s (
    id_inspecao   BIGSERIAL PRIMARY KEY,
    id_setor      BIGINT NOT NULL,
    id_inspetor   BIGINT NOT NULL,
    data_inspecao DATE NOT NULL,
    turno         VARCHAR(10) NOT NULL,
    status        VARCHAR(20) NOT NULL,
    observacoes   VARCHAR(1000),
    CONSTRAINT fk_inspecao_setor FOREIGN KEY (id_setor) REFERENCES setor (id_setor)
);

CREATE INDEX idx_inspecao_setor ON inspecao_5s (id_setor);