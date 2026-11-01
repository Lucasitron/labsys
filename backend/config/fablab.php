<?php
// Configuração transversal do FabLab (kill-switches de scheduler).
return [
    'schedulers' => [
        'rh_extrato' => env('RH_EXTRATO_SCHEDULER_ENABLED', true),
        'estoque_emprestimos_atrasados' => env('ESTOQUE_EMPRESTIMO_SCHEDULER_ENABLED', true),
        'financeiro_vencidos' => env('FINANCEIRO_VENCIDOS_SCHEDULER_ENABLED', true),
        'producao_auditoria_mesas' => env('PRODUCAO_AUDITORIA_SCHEDULER_ENABLED', true),
        'notification_reenvio' => env('NOTIFICATION_REENVIO_SCHEDULER_ENABLED', true),
        'notification_expurgo' => env('NOTIFICATION_EXPURGO_SCHEDULER_ENABLED', true),
    ],
];