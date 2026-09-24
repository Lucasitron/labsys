CREATE TABLE encomenda_kanban (
    id_kanban          BIGSERIAL PRIMARY KEY,
    id_encomenda       BIGINT NOT NULL UNIQUE,
    status             VARCHAR(20) NOT NULL,
    data_entrada_status TIMESTAMP NOT NULL,
    id_responsavel     BIGINT,
    ordem              INTEGER NOT NULL,
    version            BIGINT NOT NULL DEFAULT 0
);