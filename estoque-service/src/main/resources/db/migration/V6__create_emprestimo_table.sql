-- Tabela de empréstimos de equipamentos/ferramentas.
CREATE TABLE IF NOT EXISTS emprestimo (
    id_emprestimo           BIGSERIAL PRIMARY KEY,
    id_item                 BIGINT NOT NULL,
    id_pessoa               BIGINT NOT NULL,
    quantidade              NUMERIC(12, 2) NOT NULL,
    data_emprestimo         DATE NOT NULL,
    data_devolucao_prevista DATE NOT NULL,
    data_devolucao_real     DATE,
    status                  VARCHAR(32) NOT NULL,
    observacao              VARCHAR(255),
    CONSTRAINT fk_emprestimo_item FOREIGN KEY (id_item) REFERENCES item (id_item),
    CONSTRAINT ck_emprestimo_status CHECK (status IN ('ATIVO', 'DEVOLVIDO', 'ATRASADO'))
);

CREATE INDEX IF NOT EXISTS idx_emprestimo_item ON emprestimo (id_item);
CREATE INDEX IF NOT EXISTS idx_emprestimo_status ON emprestimo (status);