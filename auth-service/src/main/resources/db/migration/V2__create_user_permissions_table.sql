-- Tabela de permissões RBAC (papel = inteiro 0-4).
CREATE TABLE IF NOT EXISTS user_permissions (
    id          BIGSERIAL PRIMARY KEY,
    id_user     BIGINT   NOT NULL,
    role        INTEGER  NOT NULL CHECK (role BETWEEN 0 AND 4),
    active      BOOLEAN  NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS ix_user_permissions_id_user ON user_permissions (id_user);