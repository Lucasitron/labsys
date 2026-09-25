-- Registro de ponto diário consolidado a partir dos eventos RFID.
CREATE TABLE IF NOT EXISTS registro_ponto_diario (
    id              BIGSERIAL PRIMARY KEY,
    id_funcionario  BIGINT      NOT NULL REFERENCES funcionario (id),
    data            DATE        NOT NULL,
    hora_entrada    TIMESTAMPTZ,
    hora_saida      TIMESTAMPTZ,
    total_horas     NUMERIC(5,2),
    CONSTRAINT uk_ponto_funcionario_data UNIQUE (id_funcionario, data)
);