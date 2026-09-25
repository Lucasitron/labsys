CREATE TABLE historico_uso_maquina (
    id_uso         BIGSERIAL PRIMARY KEY,
    id_maquina     BIGINT NOT NULL,
    id_funcionario BIGINT NOT NULL,
    data_inicio    TIMESTAMP NOT NULL,
    data_fim       TIMESTAMP,
    horas_uso      NUMERIC(10, 2),
    observacao     VARCHAR(500),
    CONSTRAINT fk_uso_maquina FOREIGN KEY (id_maquina) REFERENCES maquina (id_maquina)
);

CREATE INDEX idx_uso_maquina ON historico_uso_maquina (id_maquina);