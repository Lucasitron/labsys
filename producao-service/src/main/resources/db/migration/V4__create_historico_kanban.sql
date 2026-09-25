CREATE TABLE historico_kanban (
    id_historico    BIGSERIAL PRIMARY KEY,
    id_encomenda    BIGINT NOT NULL,
    status_anterior VARCHAR(20),
    status_novo     VARCHAR(20) NOT NULL,
    data_alteracao  TIMESTAMP NOT NULL,
    id_usuario      BIGINT,
    observacao      VARCHAR(500)
);

CREATE INDEX idx_historico_kanban_encomenda ON historico_kanban (id_encomenda);