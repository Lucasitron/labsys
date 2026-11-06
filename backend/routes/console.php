<?php

use App\Services\Auth\TokenBlacklistService;
use App\Services\Estoque\EmprestimoService;
use App\Services\Financeiro\LancamentoService;
use App\Services\Notification\NotificationDispatchService;
use App\Models\Producao\Parametro5S;
use App\Services\Producao\ProjetoMesaService;
use App\Services\Rh\ExtratoService;
use Illuminate\Support\Facades\Log;
use Illuminate\Support\Facades\Schedule;

// Auth: purge diário 03:00 — remove só tokens expirados da blacklist
// (equivale ao @Scheduled "0 0 3 * * *" do TokenBlacklistCleanupTask).
Schedule::call(
    fn () => app(TokenBlacklistService::class)->cleanupExpired()
)->dailyAt('03:00')->name('auth:purge-blacklist');

// RH: extrato mensal dia 1 às 06:00 (conversão do Spring "0 0 6 1 * *" — 6 campos —
// p/ o cron Laravel de 5 campos). Flag RH_EXTRATO_SCHEDULER_ENABLED (default on).
if (config('fablab.schedulers.rh_extrato', true)) {
    Schedule::call(
        fn () => app(ExtratoService::class)->gerarExtratoMensal()
    )->cron('0 6 1 * *')->name('rh:extrato-mensal');
}

// Estoque: empréstimos vencidos → emprestimo.atrasado.event, diário 03:00
// (conversão do Spring "0 0 3 * * *" — 6 campos — p/ o dailyAt do Laravel).
// Flag ESTOQUE_EMPRESTIMO_SCHEDULER_ENABLED (default on).
if (config('fablab.schedulers.estoque_emprestimos_atrasados', true)) {
    Schedule::call(
        fn () => app(EmprestimoService::class)->verificarAtrasados()
    )->dailyAt('03:00')->name('estoque:emprestimos-atrasados');
}

// Financeiro: lançamentos vencidos → lancamento.vencido.event, diário 03:00
// (delta consciente: o default Java era 08:00; o monólito padroniza 03:00).
// Flag FINANCEIRO_VENCIDOS_SCHEDULER_ENABLED (default on).
if (config('fablab.schedulers.financeiro_vencidos', true)) {
    Schedule::call(
        fn () => app(LancamentoService::class)->emitirVencidos()
    )->dailyAt('03:00')->name('financeiro:vencidos');
}

// Producao: mesas ativas sem evolução → warn p/ auditoria manual do Admin,
// diário 06:00 (horário preservado do Java `0 0 6 * * *`, não o padrão 03:00).
// Flag PRODUCAO_AUDITORIA_SCHEDULER_ENABLED (default on).
if (config('fablab.schedulers.producao_auditoria_mesas', true)) {
    Schedule::call(function (): void {
        $dias = Parametro5S::inteiroDe('diasParaAuditoriaProjeto', 15);
        $pendentes = array_map(
            fn ($mesa) => (int) $mesa->getKey(),
            app(ProjetoMesaService::class)->projetosSemEvolucao(today()->subDays($dias)),
        );

        if ($pendentes !== []) {
            Log::warning('Projetos de mesa sem evolução há mais de '.$dias.' dias (revisar auditoria): '.implode(',', $pendentes));
        }
    })->dailyAt('06:00')->name('producao:auditoria-mesas');
}

// Notification: reenvio de e-mails PENDENTE a cada 30 min (equivale ao
// `*/30 * * * *` do despacho) + expurgo do histórico >90d diário 03:00
// (padrão 03:00 do monólito; docs/07 §5 pede só "job diário", sem hora).
// Flags NOTIFICATION_REENVIO/EXPURGO_SCHEDULER_ENABLED (default on).
if (config('fablab.schedulers.notification_reenvio', true)) {
    Schedule::call(
        fn () => app(NotificationDispatchService::class)->reenviarPendentes()
    )->everyThirtyMinutes()->name('notification:reenvio');
}

if (config('fablab.schedulers.notification_expurgo', true)) {
    Schedule::call(
        fn () => app(NotificationDispatchService::class)->expurgarHistorico()
    )->dailyAt('03:00')->name('notification:expurgo');
}
