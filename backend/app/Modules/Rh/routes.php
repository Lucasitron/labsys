<?php

use App\Modules\Rh\Http\Controllers\ApontamentoHorasController;
use App\Modules\Rh\Http\Controllers\CertificadoController;
use App\Modules\Rh\Http\Controllers\ExtratoController;
use App\Modules\Rh\Http\Controllers\FuncionarioController;
use App\Modules\Rh\Http\Controllers\HorasController;
use App\Modules\Rh\Http\Controllers\NivelController;
use App\Modules\Rh\Http\Controllers\PessoaController;
use App\Modules\Rh\Http\Controllers\ProcessoSeletivoController;
use App\Modules\Rh\Http\Controllers\TreinamentoController;
use Illuminate\Support\Facades\Route;

// Módulo Rh — contrato PT com o front (base do serviço Java → /api/rh no monólito).
// Tudo autenticado (auth:api + blacklist). Admin-only (can:admin): excluir pessoa,
// vínculo/nível, matriz/convites, decidir certificados. Validar horas: Admin ou
// tutor (service); demais escopos "próprios" filtram no service (server-side).

Route::prefix('rh')->middleware(['auth:api', 'blacklist'])->group(function (): void {
    // Pessoas (F1): Recrutando sem acesso; Estagiário só o próprio (service).
    Route::post('pessoas', [PessoaController::class, 'store']);
    Route::get('pessoas', [PessoaController::class, 'index']);
    Route::get('pessoas/{id}/detalhe', [PessoaController::class, 'detalhe']);
    Route::get('pessoas/{id}', [PessoaController::class, 'show']);
    Route::put('pessoas/{id}', [PessoaController::class, 'update']);
    Route::delete('pessoas/{id}', [PessoaController::class, 'destroy'])->middleware('can:admin');

    // Funcionários/níveis (F2/F7): vínculo e nível são Admin.
    Route::get('funcionarios', [FuncionarioController::class, 'index'])->middleware('can:admin');
    Route::post('funcionarios', [FuncionarioController::class, 'store'])->middleware('can:admin');
    Route::put('funcionarios/{id}/nivel', [FuncionarioController::class, 'alterarNivel'])->middleware('can:admin');
    Route::get('funcionarios/{id}/horas', [FuncionarioController::class, 'horas']);

    Route::middleware('can:admin')->group(function (): void {
        Route::get('niveis', [NivelController::class, 'matriz']);
        Route::patch('niveis/{id}/membros', [NivelController::class, 'alterarMembro']);
        Route::post('niveis/convites', [NivelController::class, 'convidar']);
    });

    // Apontamentos (F4) + fachada de horas (mesmo service, sem classe extra).
    Route::post('apontamentos-horas', [ApontamentoHorasController::class, 'store']);
    Route::get('apontamentos-horas', [ApontamentoHorasController::class, 'index']);
    Route::put('apontamentos-horas/{id}/validar', [ApontamentoHorasController::class, 'validar']);

    Route::get('horas/disponiveis', [HorasController::class, 'disponiveis']);
    Route::get('horas', [HorasController::class, 'index']);
    Route::post('horas', [HorasController::class, 'store']);
    Route::patch('horas/{id}/validar', [HorasController::class, 'validar']);
    Route::patch('horas/{id}/rejeitar', [HorasController::class, 'rejeitar']);

    // Processo seletivo (F5) + treinamentos (F6).
    Route::post('processo-seletivo', [ProcessoSeletivoController::class, 'store']);
    Route::get('processo-seletivo', [ProcessoSeletivoController::class, 'index']);
    Route::put('processo-seletivo/{id}', [ProcessoSeletivoController::class, 'update']);
    Route::post('processo-seletivo/grupos', [ProcessoSeletivoController::class, 'criarGrupo']);
    Route::patch('processo-seletivo/{id}/estagio', [ProcessoSeletivoController::class, 'moverEstagio']);
    Route::post('processo-seletivo/{id}/membros/{pessoaId}/avaliar', [ProcessoSeletivoController::class, 'avaliar']);

    Route::post('treinamentos', [TreinamentoController::class, 'store']);
    Route::post('treinamentos/{id}/avaliacoes', [TreinamentoController::class, 'avaliar']);
    Route::get('treinamentos/{id}/avaliacoes', [TreinamentoController::class, 'avaliacoes']);

    // Certificados (F8): decidir é Admin.
    Route::post('certificados/solicitar', [CertificadoController::class, 'solicitar']);
    Route::get('certificados/solicitacoes', [CertificadoController::class, 'solicitacoes']);
    Route::put('certificados/solicitacoes/{id}/aprovar', [CertificadoController::class, 'aprovar'])->middleware('can:admin');
    Route::put('certificados/solicitacoes/{id}/rejeitar', [CertificadoController::class, 'rejeitar'])->middleware('can:admin');
    Route::get('certificados/emitidos', [CertificadoController::class, 'emitidos']);
    Route::get('certificados/emitidos/{id}', [CertificadoController::class, 'emitido']);

    // Extrato (F9): leitura; geração é o scheduler (R10).
    Route::get('extrato-mensal-horas', [ExtratoController::class, 'show']);
});
