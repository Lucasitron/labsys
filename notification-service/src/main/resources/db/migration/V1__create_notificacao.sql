CREATE TABLE notificacao (
    id_notificacao  BIGSERIAL PRIMARY KEY,
    id_destinatario BIGINT,
    canal           VARCHAR(20) NOT NULL,
    tipo_evento     VARCHAR(40) NOT NULL,
    assunto         VARCHAR(255) NOT NULL,
    mensagem        VARCHAR(2000) NOT NULL,
    id_referencia   BIGINT,
    status          VARCHAR(20) NOT NULL,
    data_criacao    TIMESTAMP NOT NULL,
    data_envio      TIMESTAMP,
    data_leitura    TIMESTAMP
);

CREATE INDEX idx_notificacao_destinatario ON notificacao (id_destinatario);
CREATE INDEX idx_notificacao_status ON notificacao (status);
