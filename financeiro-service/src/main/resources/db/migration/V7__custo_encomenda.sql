CREATE TABLE custo_encomenda (
    id_custo BIGSERIAL PRIMARY KEY,
    id_encomenda INTEGER NOT NULL,
    custo_materiais NUMERIC(14, 2) NOT NULL,
    custo_mao_obra NUMERIC(14, 2) NOT NULL,
    custo_overhead NUMERIC(14, 2) NOT NULL,
    custo_total NUMERIC(14, 2) NOT NULL,
    valor_venda NUMERIC(14, 2) NOT NULL,
    margem_lucro NUMERIC(14, 2) NOT NULL,
    data_calculo DATE NOT NULL
);
CREATE INDEX idx_custo_encomenda_id_encomenda ON custo_encomenda (id_encomenda);
