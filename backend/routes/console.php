<?php

use App\Modules\Auth\Services\TokenBlacklistService;
use App\Modules\Estoque\Services\EmprestimoService;
use App\Modules\Financeiro\Services\LancamentoService;
use App\Modules\Rh\Services\ExtratoService;
use Illuminate\Support\Facades\Schedule;

// Auth: purge diário 03:00 — remove só tokens expirados da blacklist
// (equivale ao @Scheduled "0 0 3 * * *" do TokenBlacklistCleanupTask).
Schedule::call(
    fn () => app(TokenBlacklistService::class)->cleanupExpired()
)->dailyAt('03:00')->name('auth:purge-blacklist');

// RH: extrato mensal dia 1 às 06:00 (conversão do Spring "0 0 6 1 * *" — 6 campos —
// p/ o cron Laravel de 5 campos). Flag RH_EXTRATO_SCHEDULER_ENABLED (default on).
if (config('rh.extrato_scheduler_enabled', true)) {
    Schedule::call(
        fn () => app(ExtratoService::class)->gerarExtratoMensal()
    )->cron('0 6 1 * *')->name('rh:extrato-mensal');
}

// Estoque: empréstimos vencidos → emprestimo.atrasado.event, diário 03:00
// (conversão do Spring "0 0 3 * * *" — 6 campos — p/ o dailyAt do Laravel).
// Flag ESTOQUE_EMPRESTIMO_SCHEDULER_ENABLED (default on).
if (config('estoque.emprestimo_scheduler_enabled', true)) {
    Schedule::call(
        fn () => app(EmprestimoService::class)->verificarAtrasados()
    )->dailyAt('03:00')->name('estoque:emprestimos-atrasados');
}

// Financeiro: lançamentos vencidos → lancamento.vencido.event, diário 03:00
// (delta consciente: o default Java era 08:00; o monólito padroniza 03:00).
// Flag FINANCEIRO_VENCIDOS_SCHEDULER_ENABLED (default on).
if (config('financeiro.vencidos_scheduler_enabled', true)) {
    Schedule::call(
        fn () => app(LancamentoService::class)->emitirVencidos()
    )->dailyAt('03:00')->name('financeiro:vencidos');
}
