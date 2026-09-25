-- Parâmetros globais do sistema (KV) + seed de defaults.
-- Dono: auth-service. Alimenta Produção (cadência 5S) por leitura.
CREATE TABLE IF NOT EXISTS parametro_sistema (
    chave VARCHAR(128) NOT NULL,
    valor VARCHAR(2048) NOT NULL,
    CONSTRAINT pk_parametro_sistema PRIMARY KEY (chave)
);

INSERT INTO parametro_sistema (chave, valor) VALUES
    ('identidade.nomeFablab', 'FabLab IFPR — Curitiba'),
    ('identidade.logo', ''),
    ('cadencia.checklist5S', 'Semanal'),
    ('cadencia.auditoria5S', 'Mensal');
