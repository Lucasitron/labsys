CREATE TABLE solicitacao_edicao (
    id_solicitacao SERIAL PRIMARY KEY,
    tipo VARCHAR(32) NOT NULL
        CHECK (tipo IN ('ALTERACAO_DADOS', 'MUDANCA_STATUS', 'MOVER_ENCOMENDA', 'OUTRA')),
    alvo_tipo VARCHAR(16) NOT NULL CHECK (alvo_tipo IN ('CLIENTE', 'ORCAMENTO', 'ENCOMENDA')),
    alvo_id BIGINT NOT NULL,
    campo VARCHAR(128) NOT NULL,
    valor_atual VARCHAR(1000),
    valor_proposto VARCHAR(1000) NOT NULL,
    justificativa VARCHAR(1000) NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('Pendente', 'Aprovada', 'Rejeitada')),
    solicitante_id BIGINT NOT NULL,
    decidido_por BIGINT,
    motivo_decisao VARCHAR(1000),
    data_criacao TIMESTAMP NOT NULL,
    data_decisao TIMESTAMP
);
CREATE INDEX idx_solicitacao_status ON solicitacao_edicao (status);
