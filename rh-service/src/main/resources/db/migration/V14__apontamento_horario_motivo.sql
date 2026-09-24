-- Horário do apontamento (para recálculo a partir de início/fim) e motivo de rejeição.
ALTER TABLE apontamento_horas ADD COLUMN IF NOT EXISTS hora_inicio TIME;
ALTER TABLE apontamento_horas ADD COLUMN IF NOT EXISTS hora_fim TIME;
ALTER TABLE apontamento_horas ADD COLUMN IF NOT EXISTS motivo_rejeicao TEXT;
