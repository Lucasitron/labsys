CREATE TABLE parametro_overhead (
    id_parametro BIGSERIAL PRIMARY KEY,
    valor_taxa_hora NUMERIC(12, 4) NOT NULL CHECK (valor_taxa_hora >= 0),
    data_vigencia DATE NOT NULL
);
