-- Tabela de itens de inventário (insumos, ferramentas, peças).
CREATE TABLE IF NOT EXISTS item (
    id_item          BIGSERIAL PRIMARY KEY,
    nome             VARCHAR(255) NOT NULL,
    descricao        VARCHAR(255),
    categoria        VARCHAR(32)  NOT NULL,
    unidade_medida   VARCHAR(32)  NOT NULL,
    quantidade_atual NUMERIC(12, 2) NOT NULL,
    estoque_minimo   NUMERIC(12, 2) NOT NULL,
    versao           BIGINT NOT NULL DEFAULT 0,
    localizacao_id   BIGINT,
    CONSTRAINT fk_item_localizacao FOREIGN KEY (localizacao_id) REFERENCES localizacao (id_localizacao),
    CONSTRAINT ck_item_categoria CHECK (categoria IN ('INSUMO', 'FERRAMENTA', 'PECA'))
);

CREATE INDEX IF NOT EXISTS idx_item_localizacao ON item (localizacao_id);