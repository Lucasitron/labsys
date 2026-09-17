-- Valor da hora de trabalho por nível de acesso (0-3)
CREATE TABLE valor_hora_nivel (
    id_valor_hora BIGSERIAL PRIMARY KEY,
    nivel_acesso INTEGER NOT NULL CHECK (nivel_acesso BETWEEN 0 AND 3),
    valor_hora NUMERIC(12, 2) NOT NULL,
    data_vigencia DATE NOT NULL
);

CREATE INDEX idx_valor_hora_nivel_vigencia ON valor_hora_nivel (nivel_acesso, data_vigencia);