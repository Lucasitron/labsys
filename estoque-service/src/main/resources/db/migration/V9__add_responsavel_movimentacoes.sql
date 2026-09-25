-- Responsável pelo registro das movimentações (E-3/D-2).
ALTER TABLE entrada_estoque ADD COLUMN IF NOT EXISTS responsavel VARCHAR(255);
ALTER TABLE saida_estoque ADD COLUMN IF NOT EXISTS responsavel VARCHAR(255);
ALTER TABLE emprestimo ADD COLUMN IF NOT EXISTS responsavel VARCHAR(255);
