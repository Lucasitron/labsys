<?php

use App\Http\Controllers\Estoque\BomController;
use App\Http\Controllers\Estoque\EmprestimoController;
use App\Http\Controllers\Estoque\EntradaController;
use App\Http\Controllers\Estoque\FornecedorController;
use App\Http\Controllers\Estoque\ItemController;
use App\Http\Controllers\Estoque\LocalizacaoController;
use App\Http\Controllers\Estoque\SaidaController;
use Illuminate\Support\Facades\Route;

// Módulo Estoque — contrato PT com o front (base do serviço Java → /api/estoque).
// Tudo autenticado (auth:api + blacklist). Recrutando sem acesso ao módulo
// (service). Escrita itens/entradas/saídas/empréstimos: Admin ou responsável
// (service); BOM: Admin ou Bolsista responsável (service); fornecedores/
// localizações: só Admin (can:admin). Sem `?status=` em entradas/saídas (E-1).

Route::prefix('estoque')->middleware(['auth:api', 'blacklist'])->group(function (): void {
    // Itens: filtros ?categoria=&idLocalizacao=&baixo=. /export antes de /{id}.
    Route::post('itens', [ItemController::class, 'store']);
    Route::get('itens', [ItemController::class, 'index']);
    Route::get('itens/export', [ItemController::class, 'export']);
    Route::post('itens/import', [ItemController::class, 'import']);
    Route::get('itens/{id}', [ItemController::class, 'show']);
    Route::put('itens/{id}', [ItemController::class, 'update']);

    // Entradas/saídas: só ?idItem=.
    Route::post('entradas', [EntradaController::class, 'store']);
    Route::get('entradas', [EntradaController::class, 'index']);
    Route::get('entradas/{id}', [EntradaController::class, 'show']);

    Route::post('saidas', [SaidaController::class, 'store']);
    Route::get('saidas', [SaidaController::class, 'index']);
    Route::get('saidas/{id}', [SaidaController::class, 'show']);

    // Empréstimos: ?status=ativos|atrasados|historico. /atrasados antes de /{id}.
    Route::post('emprestimos', [EmprestimoController::class, 'store']);
    Route::get('emprestimos', [EmprestimoController::class, 'index']);
    Route::get('emprestimos/atrasados', [EmprestimoController::class, 'atrasados']);
    Route::get('emprestimos/{id}', [EmprestimoController::class, 'show']);
    Route::put('emprestimos/{id}/devolucao', [EmprestimoController::class, 'devolver']);

    // Fornecedores/localizações: escrita só Admin.
    Route::get('fornecedores', [FornecedorController::class, 'index']);
    Route::post('fornecedores', [FornecedorController::class, 'store'])->middleware('can:admin');

    Route::get('localizacoes', [LocalizacaoController::class, 'index']);
    Route::post('localizacoes', [LocalizacaoController::class, 'store'])->middleware('can:admin');

    // BOMs: ?projetoId= filtra pelo produto/serviço.
    Route::post('boms', [BomController::class, 'store']);
    Route::get('boms', [BomController::class, 'index']);
    Route::get('boms/{id}', [BomController::class, 'show']);
    Route::put('boms/{id}', [BomController::class, 'update']);
    Route::post('boms/{id}/consumo', [BomController::class, 'consumo']);
});
