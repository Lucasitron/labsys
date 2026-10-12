<?php

use App\Modules\Producao\Http\Controllers\AdvertenciaController;
use App\Modules\Producao\Http\Controllers\AuditoriaProjetoMesaController;
use App\Modules\Producao\Http\Controllers\ConsumoController;
use App\Modules\Producao\Http\Controllers\Inspecao5SController;
use App\Modules\Producao\Http\Controllers\KanbanController;
use App\Modules\Producao\Http\Controllers\MaquinaController;
use App\Modules\Producao\Http\Controllers\Parametro5SController;
use App\Modules\Producao\Http\Controllers\ProjetoController;
use App\Modules\Producao\Http\Controllers\ProjetoMesaController;
use App\Modules\Producao\Http\Controllers\SetorController;
use App\Modules\Producao\Http\Controllers\TarefaController;
use Illuminate\Support\Facades\Route;

// Módulo Producao — contrato PT com o front (base do serviço Java → /api/producao).
// Tudo autenticado (auth:api + blacklist); leitura exige não-Recrutando
// (service). Vínculos (responsável/atribuído/dono) no service (403); rotas
// Admin-only levam `can:admin`. `PATCH /tarefas/{id}` conclui a própria
// tarefa (compat M8). Sem CSV/PDF; sem peso/reordenação de checklist.

Route::prefix('producao')->middleware(['auth:api', 'blacklist'])->group(function (): void {
    // Projetos (F1) + tarefas (F2): vínculo do responsável no service.
    Route::post('projetos', [ProjetoController::class, 'store']);
    Route::get('projetos', [ProjetoController::class, 'index']);
    Route::get('projetos/{id}', [ProjetoController::class, 'show']);
    Route::put('projetos/{id}', [ProjetoController::class, 'update']);
    Route::put('projetos/{id}/status', [ProjetoController::class, 'alterarStatus']);
    Route::delete('projetos/{id}', [ProjetoController::class, 'destroy']);

    Route::post('tarefas', [TarefaController::class, 'store']);
    Route::get('tarefas', [TarefaController::class, 'index']);
    Route::get('tarefas/{id}', [TarefaController::class, 'show']);
    Route::put('tarefas/{id}', [TarefaController::class, 'update']);
    Route::put('tarefas/{id}/status', [TarefaController::class, 'alterarStatus']);
    Route::patch('tarefas/{id}', [TarefaController::class, 'concluir']);
    Route::delete('tarefas/{id}', [TarefaController::class, 'destroy']);

    // Kanban (F3/F11): incluir = Admin+Bolsista (service); mover/remover =
    // responsável do cartão (service); `version` no mover.
    Route::post('kanban', [KanbanController::class, 'store']);
    Route::get('kanban', [KanbanController::class, 'index']);
    Route::get('kanban/encomenda/{idEncomenda}', [KanbanController::class, 'porEncomenda']);
    Route::get('kanban/encomenda/{idEncomenda}/historico', [KanbanController::class, 'historico']);
    Route::put('kanban/{id}/mover', [KanbanController::class, 'mover']);
    Route::delete('kanban/{id}', [KanbanController::class, 'destroy']);

    Route::post('kanban/encomenda/{idEncomenda}/consumo', [ConsumoController::class, 'store']);
    Route::get('kanban/encomenda/{idEncomenda}/consumo', [ConsumoController::class, 'index']);

    // Máquinas (F4): cadastro/status Admin; uso B/V/E (service).
    Route::post('maquinas', [MaquinaController::class, 'store'])->middleware('can:admin');
    Route::get('maquinas', [MaquinaController::class, 'index']);
    Route::get('maquinas/{id}', [MaquinaController::class, 'show']);
    Route::put('maquinas/{id}', [MaquinaController::class, 'update'])->middleware('can:admin');
    Route::put('maquinas/{id}/status', [MaquinaController::class, 'alterarStatus'])->middleware('can:admin');
    Route::post('maquinas/{id}/uso', [MaquinaController::class, 'iniciarUso']);
    Route::put('maquinas/{id}/uso/{idUso}/fim', [MaquinaController::class, 'encerrarUso']);
    Route::get('maquinas/{id}/historico', [MaquinaController::class, 'historico']);
    Route::delete('maquinas/{id}', [MaquinaController::class, 'destroy'])->middleware('can:admin');

    // Setores (F5): escrita = Admin ou responsável ativo (service); designação = Admin.
    Route::post('setores', [SetorController::class, 'store']);
    Route::get('setores', [SetorController::class, 'index']);
    Route::get('setores/{id}', [SetorController::class, 'show']);
    Route::put('setores/{id}', [SetorController::class, 'update']);
    Route::delete('setores/{id}', [SetorController::class, 'destroy']);
    Route::post('setores/{id}/materiais', [SetorController::class, 'adicionarMaterial']);
    Route::delete('setores/{id}/materiais/{idMaterial}', [SetorController::class, 'removerMaterial']);
    Route::post('setores/{id}/sinalizacoes', [SetorController::class, 'adicionarSinalizacao']);
    Route::delete('setores/{id}/sinalizacoes/{idSinalizacao}', [SetorController::class, 'removerSinalizacao']);
    Route::post('setores/{id}/checklist', [SetorController::class, 'adicionarChecklist']);
    Route::put('setores/{id}/checklist/{idItem}', [SetorController::class, 'atualizarChecklist']);
    Route::delete('setores/{id}/checklist/{idItem}', [SetorController::class, 'removerChecklist']);
    Route::post('setores/{id}/responsaveis', [SetorController::class, 'adicionarResponsavel'])->middleware('can:admin');
    Route::get('setores/{id}/responsaveis', [SetorController::class, 'listarResponsaveis']);
    Route::delete('setores/{id}/responsaveis/{idResponsavel}', [SetorController::class, 'removerResponsavel']);

    // Inspeções (F6) + advertências (F7): registro Admin; inspeção pelo inspetor (service).
    Route::post('inspecoes-5s', [Inspecao5SController::class, 'store']);
    Route::get('inspecoes-5s', [Inspecao5SController::class, 'index']);
    Route::get('inspecoes-5s/{id}', [Inspecao5SController::class, 'show']);
    Route::delete('inspecoes-5s/{id}', [Inspecao5SController::class, 'destroy'])->middleware('can:admin');

    Route::post('advertencias', [AdvertenciaController::class, 'store'])->middleware('can:admin');
    Route::get('advertencias', [AdvertenciaController::class, 'index']);
    Route::get('advertencias/{idFuncionario}', [AdvertenciaController::class, 'porFuncionario']);

    // Mesas (F8): dono ou Admin (service); auditoria Admin.
    Route::post('projetos-mesa', [ProjetoMesaController::class, 'store']);
    Route::get('projetos-mesa', [ProjetoMesaController::class, 'index']);
    Route::get('projetos-mesa/{id}', [ProjetoMesaController::class, 'show']);
    Route::put('projetos-mesa/{id}', [ProjetoMesaController::class, 'update']);
    Route::put('projetos-mesa/{id}/evolucao', [ProjetoMesaController::class, 'evolucao']);
    Route::get('projetos-mesa/{id}/qrcode', [ProjetoMesaController::class, 'qrcode']);
    Route::get('projetos-mesa/{id}/auditorias', [ProjetoMesaController::class, 'auditorias']);
    Route::delete('projetos-mesa/{id}', [ProjetoMesaController::class, 'destroy']);

    Route::post('auditorias-projeto-mesa', [AuditoriaProjetoMesaController::class, 'store'])->middleware('can:admin');
    Route::get('auditorias-projeto-mesa/{idProjetoMesa}', [AuditoriaProjetoMesaController::class, 'porMesa']);

    // Parâmetros (F9): leitura geral; `PUT` Admin.
    Route::get('parametros-5s', [Parametro5SController::class, 'index']);
    Route::get('parametros-5s/{id}', [Parametro5SController::class, 'show']);
    Route::put('parametros-5s/{id}', [Parametro5SController::class, 'update'])->middleware('can:admin');
});
