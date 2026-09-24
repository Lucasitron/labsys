-- Tabela de tokens JWT invalidados (logout).
CREATE TABLE IF NOT EXISTS token_blacklist (
    id          BIGSERIAL PRIMARY KEY,
    token       TEXT         NOT NULL,
    expiry_date TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_token_blacklist_token UNIQUE (token)
);