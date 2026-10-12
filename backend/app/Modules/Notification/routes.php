<?php

use App\Modules\Notification\Http\Controllers\ConfiguracaoCanalController;
use App\Modules\Notification\Http\Controllers\HistoricoController;
use App\Modules\Notification\Http\Controllers\NotificacaoController;
use App\Modules\Notification\Http\Controllers\PreferenciaController;
use App\Modules\Notification\Http\Controllers\RevisaoController;
use Illuminate\Support\Facades\Route;

// Módulo Notification — CONSUMIDOR central (sem produtor próprio). Contrato PT
// com o front; saída com keys EN 1:1 (D-1). Inbox = qualquer autenticado
// (incl. Recrutando); resto = Admin (`can:admin`).
// Alias `GET /api/notificacoes` (+contagem): o App Shell/sino consome esse path.

Route::prefix('notification')->middleware(['auth:api', 'blacklist'])->group(function (): void {
    Route::get('notificacoes', [NotificacaoController::class, 'index']);
    Route::get('notificacoes/nao-lidas/contagem', [NotificacaoController::class, 'naoLidas']);
    Route::patch('notificacoes/{id}/ler', [NotificacaoController::class, 'ler']);
    Route::post('notificacoes/ler-todas', [NotificacaoController::class, 'lerTodas']);

    Route::post('notificacoes/revisar', [RevisaoController::class, 'store'])->middleware('can:admin');
    Route::get('historico', [HistoricoController::class, 'index'])->middleware('can:admin');

    Route::get('preferencias', [PreferenciaController::class, 'index'])->middleware('can:admin');
    Route::put('preferencias', [PreferenciaController::class, 'update'])->middleware('can:admin');

    Route::get('configuracao-canais', [ConfiguracaoCanalController::class, 'index'])->middleware('can:admin');
    Route::put('configuracao-canais/{canal}', [ConfiguracaoCanalController::class, 'update'])->middleware('can:admin');
    Route::put('configuracao-canais', [ConfiguracaoCanalController::class, 'update'])->middleware('can:admin');
});

Route::prefix('notificacoes')->middleware(['auth:api', 'blacklist'])->group(function (): void {
    Route::get('', [NotificacaoController::class, 'index']);
    Route::get('nao-lidas/contagem', [NotificacaoController::class, 'naoLidas']);
});
