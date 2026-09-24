CREATE TABLE setor_checklist (
    id_checklist BIGSERIAL PRIMARY KEY,
    id_setor     BIGINT NOT NULL,
    item         VARCHAR(255) NOT NULL,
    ativo        BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_checklist_setor FOREIGN KEY (id_setor) REFERENCES setor (id_setor)
);

CREATE INDEX idx_checklist_setor ON setor_checklist (id_setor);