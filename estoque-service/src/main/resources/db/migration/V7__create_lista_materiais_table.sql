-- Tabela de Listas de Materiais (BOM).
CREATE TABLE IF NOT EXISTS lista_materiais (
    id_bom             BIGSERIAL PRIMARY KEY,
    id_produto_servico BIGINT NOT NULL,
    nome               VARCHAR(255) NOT NULL,
    versao             INTEGER NOT NULL,
    editavel           BOOLEAN NOT NULL
);