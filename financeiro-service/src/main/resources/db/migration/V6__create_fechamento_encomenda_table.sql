-- Fechamento de encomenda (valores congelados)
CREATE TABLE fechamento_encomenda (
    id_fechamento BIGSERIAL PRIMARY KEY,
    id_encomenda BIGINT NOT NULL UNIQUE,
    horas_estimadas NUMERIC(12, 2),
    valor_fechado NUMERIC(14, 2) NOT NULL,
    data_fechamento DATE NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('ABERTA', 'CONCLUIDA', 'CANCELADA')),
    horas_validadas NUMERIC(14, 2) NOT NULL DEFAULT 0
);