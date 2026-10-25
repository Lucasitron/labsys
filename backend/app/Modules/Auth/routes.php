<?php

use App\Modules\Auth\Http\Controllers\AuthController;
use App\Modules\Auth\Http\Controllers\ConfiguracaoSistemaController;
use App\Modules\Auth\Http\Controllers\PermissaoController;
use App\Modules\Auth\Http\Controllers\TokenIntegracaoController;
use App\Modules\Auth\Http\Controllers\UsuarioController;
use Illuminate\Support\Facades\Route;

// Módulo Auth — contrato PT com o front (base /auth do serviço → /api/auth no monólito).
// Públicas: login/validate-rfid/refresh (+ GET /up health do bootstrap).
// Autenticadas (auth:api + blacklist): logout/me/senha.
// Admin-only (can:admin): permissions/usuarios/permissoes/configuracoes/**.

Route::prefix('auth')->group(function (): void {
    Route::post('login', [AuthController::class, 'login']);
    Route::post('validate-rfid', [AuthController::class, 'validateRfid']);
    Route::post('refresh', [AuthController::class, 'refresh']);

    Route::middleware(['auth:api', 'blacklist'])->group(function (): void {
        Route::post('logout', [AuthController::class, 'logout']);
        Route::get('me', [AuthController::class, 'me']);
        Route::put('senha', [AuthController::class, 'alterarSenha']);
        Route::get('permissions', [AuthController::class, 'permissions'])->middleware('can:admin');
    });
});

Route::middleware(['auth:api', 'blacklist', 'can:admin'])->group(function (): void {
    Route::get('usuarios', [UsuarioController::class, 'index']);
    Route::post('usuarios', [UsuarioController::class, 'store']);
    Route::put('usuarios/{id}', [UsuarioController::class, 'update']);
    Route::patch('usuarios/{id}/status', [UsuarioController::class, 'alterarStatus']);

    Route::get('permissoes', [PermissaoController::class, 'index']);
    Route::put('permissoes/{modulo}/{nivel}', [PermissaoController::class, 'update']);

    Route::get('configuracoes/sistema', [ConfiguracaoSistemaController::class, 'show']);
    Route::put('configuracoes/sistema', [ConfiguracaoSistemaController::class, 'update']);

    Route::get('configuracoes/tokens', [TokenIntegracaoController::class, 'index']);
    Route::post('configuracoes/tokens', [TokenIntegracaoController::class, 'store']);
    Route::delete('configuracoes/tokens/{id}', [TokenIntegracaoController::class, 'destroy']);
});
