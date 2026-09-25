CREATE TABLE lancamento_financeiro (
    id_lancamento BIGSERIAL PRIMARY KEY,
    id_categoria BIGINT NOT NULL REFERENCES categoria_financeira (id_categoria),
    tipo VARCHAR(16) NOT NULL
        CHECK (tipo IN ('ENTRADA', 'SAIDA')),
    valor NUMERIC(14, 2) NOT NULL CHECK (valor > 0),
    data_vencimento DATE NOT NULL,
    data_pagamento DATE,
    status VARCHAR(16) NOT NULL
        CHECK (status IN ('PENDENTE', 'PAGO', 'ATRASADO', 'CANCELADO')),
    id_referencia_externa VARCHAR(100),
    observacao VARCHAR(1000)
);
CREATE INDEX idx_lancamento_status_vencimento ON lancamento_financeiro (status, data_vencimento);
CREATE INDEX idx_lancamento_referencia ON lancamento_financeiro (id_referencia_externa);
