-- CPF opcional da pessoa (LGPD: a API sempre responde mascarado).
ALTER TABLE pessoa ADD COLUMN IF NOT EXISTS cpf VARCHAR(11);
ALTER TABLE pessoa ADD CONSTRAINT uk_pessoa_cpf UNIQUE (cpf);
