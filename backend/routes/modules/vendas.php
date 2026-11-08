<?php

use App\Http\Controllers\Vendas\ClienteController;
use App\Http\Controllers\Vendas\EncomendaController;
use App\Http\Controllers\Vendas\HistoricoController;
use App\Http\Controllers\Vendas\InteracaoController;
use App\Http\Controllers\Vendas\MarketplaceController;
use App\Http\Controllers\Vendas\OrcamentoController;
use App\Http\Controllers\Vendas\SolicitacaoController;
use App\Http\Controllers\Vendas\TagController;
use App\Http\Controllers\Vendas\TarefaMarketingController;
use Illuminate\Support\Facades\Route;

// Módulo Vendas — contrato PT com o front (base do serviço Java → /api/vendas).
// Tudo autenticado (auth:api + blacklist). Leitura: qualquer autenticado exceto
// Recrutando (service). Escrita de domínio: só Admin (can:admin + service);
// kanban mover + nova-ordem: criador ou Admin (service, sem can:admin);
// solicitar: qualquer autenticado exceto Recrutando (service). Sem endpoint
// `converter` (é o POST /encomendas com idOrcamento); sem GET /interacoes
// global; sem CSV.

Route::prefix('vendas')->middleware(['auth:api', 'blacklist'])->group(function (): void {
    // Clientes (F1) + tags (F2): escrita Admin. /bulk-tag é Admin (rota+service).
    Route::post('clientes', [ClienteController::class, 'store'])->middleware('can:admin');
    Route::get('clientes', [ClienteController::class, 'index']);
    Route::get('clientes/{id}', [ClienteController::class, 'show']);
    Route::put('clientes/{id}', [ClienteController::class, 'update'])->middleware('can:admin');
    Route::delete('clientes/{id}', [ClienteController::class, 'destroy'])->middleware('can:admin');
    Route::post('clientes/{id}/tags', [ClienteController::class, 'vincularTag'])->middleware('can:admin');
    Route::delete('clientes/{id}/tags/{tagId}', [ClienteController::class, 'desvincularTag'])->middleware('can:admin');
    Route::post('clientes/bulk-tag', [ClienteController::class, 'bulkTag'])->middleware('can:admin');

    Route::post('tags', [TagController::class, 'store'])->middleware('can:admin');
    Route::get('tags', [TagController::class, 'index']);

    // Orçamentos (F3): escrita Admin (incl. duplicar).
    Route::post('orcamentos', [OrcamentoController::class, 'store'])->middleware('can:admin');
    Route::get('orcamentos', [OrcamentoController::class, 'index']);
    Route::get('orcamentos/{id}', [OrcamentoController::class, 'show']);
    Route::put('orcamentos/{id}', [OrcamentoController::class, 'update'])->middleware('can:admin');
    Route::post('orcamentos/{id}/duplicar', [OrcamentoController::class, 'duplicar'])->middleware('can:admin');

    // Encomendas + Kanban (F4/F5): criar é Admin; mover/nova-ordem é criador+Admin.
    Route::post('encomendas', [EncomendaController::class, 'store'])->middleware('can:admin');
    Route::get('encomendas', [EncomendaController::class, 'index']);
    Route::get('encomendas/{id}/status', [EncomendaController::class, 'status']);
    Route::get('encomendas/{id}', [EncomendaController::class, 'show']);
    Route::put('encomendas/{id}/kanban', [EncomendaController::class, 'moverKanban']);
    Route::post('encomendas/{id}/nova-ordem', [EncomendaController::class, 'novaOrdem']);

    Route::get('historico-status/{idEncomenda}', [HistoricoController::class, 'listar']);

    // Marketplace manual (F6): só ?encomendaId=&plataforma=; escrita Admin.
    Route::post('marketplace', [MarketplaceController::class, 'store'])->middleware('can:admin');
    Route::get('marketplace', [MarketplaceController::class, 'index']);

    // CRM (F7/F8): escrita Admin; interações escopadas por cliente.
    Route::post('interacoes', [InteracaoController::class, 'store'])->middleware('can:admin');
    Route::get('interacoes/{idCliente}', [InteracaoController::class, 'porCliente']);

    Route::post('tarefas-marketing', [TarefaMarketingController::class, 'store'])->middleware('can:admin');
    Route::get('tarefas-marketing', [TarefaMarketingController::class, 'index']);
    Route::put('tarefas-marketing/{id}', [TarefaMarketingController::class, 'update'])->middleware('can:admin');

    // Solicitações (D-4): pedir é aberto; decidir é Admin, sem auto-apply.
    Route::post('solicitacoes', [SolicitacaoController::class, 'store']);
    Route::get('solicitacoes', [SolicitacaoController::class, 'index']);
    Route::put('solicitacoes/{id}/decisao', [SolicitacaoController::class, 'decidir'])->middleware('can:admin');
});
