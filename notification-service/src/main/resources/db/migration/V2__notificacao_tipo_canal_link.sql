-- D-1..D-7 (§6.3): tipo/canal/link para lista, filtros e histórico.
ALTER TABLE notificacao ADD COLUMN IF NOT EXISTS tipo  VARCHAR(40);
ALTER TABLE notificacao ADD COLUMN IF NOT EXISTS canal VARCHAR(20);
ALTER TABLE notificacao ADD COLUMN IF NOT EXISTS link  VARCHAR(2000);

CREATE INDEX IF NOT EXISTS ix_notificacao_usuario_tipo ON notificacao (id_usuario, tipo);
