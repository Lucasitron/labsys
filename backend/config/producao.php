<?php

// Producao: auditoria de mesas sem evolução (equivale a
// producao.auditoria.scheduler.enabled + cron 06:00 do Java, preservado).
return [
    'auditoria_scheduler_enabled' => env('PRODUCAO_AUDITORIA_SCHEDULER_ENABLED', true),
];
