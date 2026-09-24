CREATE TABLE auditoria_projeto_mesa (
    id_auditoria           BIGSERIAL PRIMARY KEY,
    id_projeto_mesa        BIGINT NOT NULL,
    data_auditoria         DATE NOT NULL,
    resultado              VARCHAR(20) NOT NULL,
    acao_tomada            VARCHAR(500),
    id_admin_responsavel   BIGINT NOT NULL,
    CONSTRAINT fk_auditoria_projeto_mesa FOREIGN KEY (id_projeto_mesa) REFERENCES projeto_mesa (id_projeto_mesa)
);

CREATE INDEX idx_auditoria_projeto_mesa ON auditoria_projeto_mesa (id_projeto_mesa);