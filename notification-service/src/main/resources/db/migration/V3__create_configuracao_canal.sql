CREATE TABLE configuracao_canal (
    id_configuracao BIGSERIAL PRIMARY KEY,
    canal           VARCHAR(20) NOT NULL UNIQUE,
    habilitado      BOOLEAN NOT NULL,
    parametros      VARCHAR(2000)
);
