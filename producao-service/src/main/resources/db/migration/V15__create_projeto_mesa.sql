CREATE TABLE projeto_mesa (
    id_projeto_mesa      BIGSERIAL PRIMARY KEY,
    id_funcionario       BIGINT NOT NULL,
    id_mesa              BIGINT NOT NULL,
    nome_projeto         VARCHAR(150) NOT NULL,
    tipo_projeto         VARCHAR(100),
    prazo_execucao       DATE,
    data_inicio          DATE NOT NULL,
    data_ultima_evolucao DATE,
    status               VARCHAR(20) NOT NULL,
    qr_code_totem       VARCHAR(500)
);