-- FabLab — criação dos bancos por microsserviço (database-per-service).
-- Executado apenas na primeira inicialização do volume do PostgreSQL.

CREATE DATABASE fablab_auth;
CREATE DATABASE fablab_rh;
CREATE DATABASE fablab_estoque;
CREATE DATABASE fablab_vendas;
CREATE DATABASE fablab_financeiro;
CREATE DATABASE fablab_producao;
CREATE DATABASE fablab_notification;

-- Concede acesso ao usuário aplicação em todos os bancos.
GRANT ALL PRIVILEGES ON DATABASE fablab_auth        TO fablab;
GRANT ALL PRIVILEGES ON DATABASE fablab_rh          TO fablab;
GRANT ALL PRIVILEGES ON DATABASE fablab_estoque     TO fablab;
GRANT ALL PRIVILEGES ON DATABASE fablab_vendas      TO fablab;
GRANT ALL PRIVILEGES ON DATABASE fablab_financeiro  TO fablab;
GRANT ALL PRIVILEGES ON DATABASE fablab_producao    TO fablab;
GRANT ALL PRIVILEGES ON DATABASE fablab_notification TO fablab;