-- Situação da conta de acesso (gestão de contas em Configurações/Usuários).
-- Contas existentes assumem ATIVO; novas contas nascem com o valor da aplicação.
ALTER TABLE login ADD COLUMN IF NOT EXISTS situacao VARCHAR(20) NOT NULL DEFAULT 'ATIVO';

ALTER TABLE login DROP CONSTRAINT IF EXISTS ck_login_situacao;
ALTER TABLE login ADD CONSTRAINT ck_login_situacao CHECK (situacao IN ('ATIVO', 'PENDENTE', 'DESATIVADO'));
