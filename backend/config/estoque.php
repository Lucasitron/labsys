<?php

// Estoque: scheduler de empréstimos atrasados (equivale a
// estoque.alerta.emprestimo.scheduler.enabled).
return [
    'emprestimo_scheduler_enabled' => env('ESTOQUE_EMPRESTIMO_SCHEDULER_ENABLED', true),
];
