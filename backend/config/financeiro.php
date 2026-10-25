<?php

// Financeiro: varredura de lançamentos vencidos (equivale a
// financeiro.vencidos.scheduler.enabled + cron; default 03:00 do monólito).
return [
    'vencidos_scheduler_enabled' => env('FINANCEIRO_VENCIDOS_SCHEDULER_ENABLED', true),
];
