<?php

// Rotas da API (contrato PT com o front). Cada módulo publica seu routes.php,
// montado aqui sob o prefixo /api/<modulo>.

require __DIR__.'/modules/auth.php';
require __DIR__.'/../app/Modules/Rh/routes.php';
require __DIR__.'/../app/Modules/Estoque/routes.php';
require __DIR__.'/../app/Modules/Vendas/routes.php';
require __DIR__.'/../app/Modules/Financeiro/routes.php';
require __DIR__.'/../app/Modules/Producao/routes.php';
require __DIR__.'/modules/notification.php';
require __DIR__.'/../app/Modules/Dashboard/routes.php';
