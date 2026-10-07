<?php

use App\Modules\Financeiro\Http\Controllers\CategoriaController;
use App\Modules\Financeiro\Http\Controllers\CompraController;
use App\Modules\Financeiro\Http\Controllers\CusteioController;
use App\Modules\Financeiro\Http\Controllers\DoacaoController;
use App\Modules\Financeiro\Http\Controllers\LancamentoController;
use App\Modules\Financeiro\Http\Controllers\ParametroController;
use App\Modules\Financeiro\Http\Controllers\RelatorioController;
use Illuminate\Support\Facades\Route;

// Módulo Financeiro — contrato PT com o front. Tudo Admin (`can:admin`,
// rbac-matrix.md regra 2): zero rota pública. Sem bulk, sem PUT em
// fechamento/parâmetros, sem visualizar de compra, sem export.
Route::prefix('financeiro')->middleware(['auth:api', 'blacklist', 'can:admin'])->group(function (): void {
    // Categorias (F1) e lançamentos (F2): filtros D-4/D-8, shapes D-2.
    Route::post('categorias', [CategoriaController::class, 'store']);
    Route::get('categorias', [CategoriaController::class, 'index']);

    Route::post('lancamentos', [LancamentoController::class, 'store']);
    Route::get('lancamentos', [LancamentoController::class, 'index']);
    Route::put('lancamentos/{id}/pagamento', [LancamentoController::class, 'pagar']);

    // Doações/recursos (F3) e compras (F8, informativo).
    Route::post('doacoes-recursos', [DoacaoController::class, 'store']);
    Route::get('doacoes-recursos', [DoacaoController::class, 'index']);

    Route::post('solicitacoes-compra', [CompraController::class, 'store']);
    Route::get('solicitacoes-compra', [CompraController::class, 'index']);
    Route::get('solicitacoes-compra/{id}', [CompraController::class, 'show']);
    Route::put('solicitacoes-compra/{id}/concluir', [CompraController::class, 'concluir']);

    // Parâmetros de custeio (F4/F6, append-only; GET overhead = vigente).
    Route::post('valores-hora', [ParametroController::class, 'definirValorHora']);
    Route::get('valores-hora', [ParametroController::class, 'valoresVigentes']);
    Route::post('parametros-overhead', [ParametroController::class, 'definirOverhead']);
    Route::get('parametros-overhead', [ParametroController::class, 'overheadVigente']);

    // Fechamento (F5, imutável) + custo congelado (F7).
    Route::post('fechamento-encomenda', [CusteioController::class, 'fechar']);
    Route::get('fechamento-encomenda', [CusteioController::class, 'listar']);
    Route::get('fechamento-encomenda/{idEncomenda}', [CusteioController::class, 'consultar']);
    Route::post('fechamento-encomenda/{idEncomenda}/nova-ordem', [CusteioController::class, 'novaOrdem']);
    Route::get('custos-encomenda/{idEncomenda}', [CusteioController::class, 'custo']);

    // Relatórios (F9, shapes servidos; sem período nos 3 últimos, como no Java).
    Route::get('relatorios/fluxo-caixa', [RelatorioController::class, 'fluxoCaixa']);
    Route::get('relatorios/dre', [RelatorioController::class, 'dre']);
    Route::get('relatorios/lucratividade', [RelatorioController::class, 'lucratividade']);
    Route::get('relatorios/inadimplencia', [RelatorioController::class, 'inadimplencia']);
    Route::get('relatorios/doacoes-despesas', [RelatorioController::class, 'doacoesDespesas']);
    Route::get('relatorios/custo-maquina', [RelatorioController::class, 'custoMaquina']);
});
