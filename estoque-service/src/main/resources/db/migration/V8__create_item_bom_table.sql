-- Tabela de itens da Lista de Materiais (BOM).
CREATE TABLE IF NOT EXISTS item_bom (
    id_item_bom        BIGSERIAL PRIMARY KEY,
    id_bom             BIGINT NOT NULL,
    id_item            BIGINT NOT NULL,
    quantidade_prevista NUMERIC(12, 2) NOT NULL,
    quantidade_real    NUMERIC(12, 2),
    CONSTRAINT uk_item_bom_bom_item UNIQUE (id_bom, id_item),
    CONSTRAINT fk_item_bom_bom FOREIGN KEY (id_bom) REFERENCES lista_materiais (id_bom),
    CONSTRAINT fk_item_bom_item FOREIGN KEY (id_item) REFERENCES item (id_item)
);

CREATE INDEX IF NOT EXISTS idx_item_bom_item ON item_bom (id_item);