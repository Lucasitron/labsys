CREATE TABLE fechamento_encomenda (
    id_fechamento BIGSERIAL PRIMARY KEY,
    id_encomenda INTEGER NOT NULL UNIQUE,
    horas_estimadas NUMERIC(14, 2) NOT NULL CHECK (horas_estimadas >= 0),
    valor_fechado NUMERIC(14, 2) NOT NULL CHECK (valor_fechado >= 0),
    data_fechamento DATE NOT NULL,
    status VARCHAR(16) NOT NULL
        CHECK (status IN ('ABERTA', 'CONCLUIDA', 'CANCELADA')),
    horas_validadas NUMERIC(14, 2) NOT NULL DEFAULT 0
);
