CREATE TABLE item_inspecao_5s (
    id_item_inspecao BIGSERIAL PRIMARY KEY,
    id_inspecao      BIGINT NOT NULL,
    id_checklist     BIGINT NOT NULL,
    conforme         BOOLEAN NOT NULL,
    observacao       VARCHAR(500),
    CONSTRAINT fk_item_inspecao FOREIGN KEY (id_inspecao) REFERENCES inspecao_5s (id_inspecao),
    CONSTRAINT fk_item_checklist FOREIGN KEY (id_checklist) REFERENCES setor_checklist (id_checklist)
);

CREATE INDEX idx_item_inspecao ON item_inspecao_5s (id_inspecao);