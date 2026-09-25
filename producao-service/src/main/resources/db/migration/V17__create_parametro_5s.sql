CREATE TABLE parametro_5s (
    id_parametro BIGSERIAL PRIMARY KEY,
    chave        VARCHAR(100) NOT NULL UNIQUE,
    valor        VARCHAR(255) NOT NULL,
    descricao    VARCHAR(500)
);

INSERT INTO parametro_5s (chave, valor, descricao) VALUES
    ('diasParaAuditoriaProjeto', '15', 'Dias sem evolução para um projeto de mesa entrar em auditoria'),
    ('diaSemanaInspecao', 'SEXTA', 'Dia da semana em que as inspeções 5S devem ocorrer'),
    ('periodoExperimentalAtivo', 'true', 'Indica se o período experimental de penalidades está ativo'),
    ('rotacaoDias', '7', 'Intervalo em dias para rotação de responsáveis pelos setores');