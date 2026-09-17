-- Parâmetro de taxa de overhead por hora
CREATE TABLE parametro_overhead (
    id_parametro BIGSERIAL PRIMARY KEY,
    valor_taxa_hora NUMERIC(12, 4) NOT NULL,
    data_vigencia DATE NOT NULL
);