CREATE TABLE setor_material (
    id_material BIGSERIAL PRIMARY KEY,
    id_setor    BIGINT NOT NULL,
    descricao   VARCHAR(255) NOT NULL,
    quantidade  NUMERIC(12, 2) NOT NULL,
    CONSTRAINT fk_material_setor FOREIGN KEY (id_setor) REFERENCES setor (id_setor)
);

CREATE INDEX idx_material_setor ON setor_material (id_setor);