CREATE TABLE historico_status_encomenda (
    id_historico SERIAL PRIMARY KEY,
    id_encomenda INTEGER NOT NULL REFERENCES encomenda (id_encomenda) ON DELETE CASCADE,
    status_anterior VARCHAR(16) NOT NULL,
    status_novo VARCHAR(16) NOT NULL,
    data_alteracao TIMESTAMP NOT NULL,
    id_usuario BIGINT NOT NULL,
    observacao VARCHAR(1000)
);
CREATE INDEX idx_historico_encomenda ON historico_status_encomenda (id_encomenda, data_alteracao);
