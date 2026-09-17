CREATE TABLE notificacao_historico (
    id_historico            BIGSERIAL PRIMARY KEY,
    id_notificacao_original BIGINT,
    id_destinatario         BIGINT,
    canal                   VARCHAR(20) NOT NULL,
    tipo_evento             VARCHAR(40) NOT NULL,
    assunto                 VARCHAR(255) NOT NULL,
    mensagem                VARCHAR(2000) NOT NULL,
    id_referencia           BIGINT,
    data_criacao            TIMESTAMP NOT NULL,
    data_envio              TIMESTAMP,
    data_leitura            TIMESTAMP,
    data_revisao_admin      TIMESTAMP NOT NULL,
    id_admin_revisor        BIGINT
);

CREATE INDEX idx_notificacao_historico_revisao ON notificacao_historico (data_revisao_admin);
CREATE INDEX idx_notificacao_historico_destinatario ON notificacao_historico (id_destinatario);
