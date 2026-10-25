<?php

use App\Modules\Dashboard\Http\Controllers\DashboardController;
use App\Modules\Producao\Http\Controllers\TarefaController;
use Illuminate\Support\Facades\Route;

// Módulo Dashboard — AGREGADOR sem persistência (sem Model/Migration).
// `GET /summary` (front `fetchSummary`) + alias PT `GET /resumo`, mesmo
// controller. Alias `PATCH /api/tasks/{id}` → Producao `concluir` (compat
// `{done:true}` ignorado, vínculo tarefa-responsável/Admin de M6, idempotente
// — sem lógica duplicada).

Route::prefix('dashboard')->middleware(['auth:api', 'blacklist', 'can:ver-resumo'])->group(function (): void {
    Route::get('summary', [DashboardController::class, 'summary']);
    Route::get('resumo', [DashboardController::class, 'summary']);
});

Route::patch('tasks/{id}', [TarefaController::class, 'concluir'])->middleware(['auth:api', 'blacklist']);
