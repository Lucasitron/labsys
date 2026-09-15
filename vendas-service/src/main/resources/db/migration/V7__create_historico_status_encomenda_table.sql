CREATE TABLE historico_status_encomenda (
    id BIGSERIAL PRIMARY KEY,
    id_encomenda BIGINT NOT NULL REFERENCES encomenda(id) ON DELETE CASCADE,
    status_anterior VARCHAR(20),
    status_novo VARCHAR(20) NOT NULL,
    data_alteracao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_usuario BIGINT,
    observacao VARCHAR(500)
);
CREATE INDEX idx_historico_encomenda ON historico_status_encomenda (id_encomenda);