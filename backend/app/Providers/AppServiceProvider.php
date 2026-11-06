<?php

namespace App\Providers;

use App\Contracts\AuthContract;
use App\Contracts\DefaultAuthContract;
use App\Enums\Auth\Role;
use App\Events\RfidAccessEvent;
use App\Models\Auth\Login;
use App\Models\Auth\UserPermission;
use App\Modules\Dashboard\Policies\DashboardPolicy;
use App\Events\CompraSolicitadaEvent;
use App\Events\ProducaoConcluidaEvent;
use App\Events\ProducaoStatusAlteradoEvent;
use App\Contracts\DefaultEstoqueContract;
use App\Contracts\EstoqueContract;
use App\Events\EmprestimoAtrasadoEvent;
use App\Events\EstoqueBaixoEvent;
use App\Listeners\Estoque\ConsumoProducaoListener;
use App\Events\LancamentoVencidoEvent;
use App\Listeners\Financeiro\EncomendaCriadaListener;
use App\Listeners\Financeiro\HorasValidadasListener;
use App\Listeners\Financeiro\ProducaoConcluidaListener;
use App\Contracts\DefaultNotificationContract;
use App\Contracts\NotificationContract;
use App\Listeners\Notification\EstoqueNotificacaoListener;
use App\Listeners\Notification\FinanceiroNotificacaoListener;
use App\Listeners\Notification\ProducaoNotificacaoListener;
use App\Listeners\Notification\RhNotificacaoListener;
use App\Listeners\Notification\VendasNotificacaoListener;
use App\Modules\Producao\Contracts\DefaultProducaoContract;
use App\Modules\Producao\Contracts\ProducaoContract;
use App\Modules\Producao\Events\AdvertenciaLimiteAtingidoEvent;
use App\Modules\Producao\Events\AdvertenciaRegistradaEvent;
use App\Modules\Producao\Events\KanbanStatusAlteradoEvent;
use App\Modules\Producao\Events\ProjetoMesaAbandonadoEvent;
use App\Modules\Producao\Listeners\EncomendaCriadaListener as ProducaoEncomendaCriadaListener;
use App\Modules\Producao\Listeners\NivelAlteradoListener;
use App\Modules\Rh\Contracts\DefaultRhContract;
use App\Modules\Rh\Contracts\RhContract;
use App\Modules\Rh\Events\CertificadoAprovadoEvent;
use App\Modules\Rh\Events\CertificadoRejeitadoEvent;
use App\Modules\Rh\Events\CertificadoSolicitadoEvent;
use App\Modules\Rh\Events\ExtratoMensalHorasEvent;
use App\Modules\Rh\Events\HorasValidadasEvent;
use App\Modules\Rh\Events\NivelAlteradoEvent;
use App\Modules\Rh\Listeners\ProcessarPontoRfid;
use App\Contracts\DefaultVendasContract;
use App\Contracts\VendasContract;
use App\Events\EncomendaCriadaEvent;
use App\Events\EncomendaStatusAlteradoEvent;
use App\Events\OrcamentoAprovadoEvent;
use App\Listeners\Vendas\ProducaoStatusListener;
use Illuminate\Http\Resources\Json\JsonResource;
use Illuminate\Support\Facades\Event;
use Illuminate\Support\Facades\Gate;
use Illuminate\Support\ServiceProvider;

class AppServiceProvider extends ServiceProvider
{
    /**
     * Register any application services.
     */
    public function register(): void
    {
        // Preserva o comportamento pré-remap: listeners viviam em app/Modules e
        // não eram auto-descobertos. Evita registro duplicado após a mudança
        // layer-first (app/Listeners passa a ser varrido pelo framework).
        \Illuminate\Foundation\Support\Providers\EventServiceProvider::disableEventDiscovery();
        // Fronteira in-process do Auth (único consumo permitido por outros módulos).
        $this->app->singleton(AuthContract::class, DefaultAuthContract::class);

        // Fronteira in-process do RH (Financeiro/Produção consomem só o Contract).
        $this->app->singleton(RhContract::class, DefaultRhContract::class);

        // Fronteira in-process do Estoque (Vendas/M4 e Produção/M6 consomem só o Contract).
        $this->app->singleton(EstoqueContract::class, DefaultEstoqueContract::class);

        // Fronteira in-process do Vendas (Produção/M6 e Financeiro/M5 consomem só o Contract).
        $this->app->singleton(VendasContract::class, DefaultVendasContract::class);

        // Fronteira in-process do Producao (Dashboard/M8 consome só o Contract).
        $this->app->singleton(ProducaoContract::class, DefaultProducaoContract::class);

        // Fronteira in-process do Notification (Dashboard/M8 consome só o Contract).
        $this->app->singleton(NotificationContract::class, DefaultNotificationContract::class);
    }

    /**
     * Bootstrap any application services.
     */
    public function boot(): void
    {
        // Recursos de API sem envelope "data" (contrato direto com o front).
        JsonResource::withoutWrapping();

        // Migrações por módulo vivem em database/migrations/<modulo>/ e são
        // registradas aqui (o migrator só varre 1 nível — sem isso, CLI e
        // RefreshDatabase ignoram os submódulos em silêncio).
        $modulos = glob(database_path('migrations/*'), GLOB_ONLYDIR) ?: [];
        if ($modulos !== []) {
            $this->loadMigrationsFrom($modulos);
        }

        // NFR-2 (JwtProperties): fail-fast se JWT_SECRET ausente ou < 32 chars.
        $secret = (string) config('jwt.secret');
        if ($secret === '' || mb_strlen($secret) < 32) {
            throw new \RuntimeException('JWT_SECRET deve ter no mínimo 32 caracteres');
        }

        // RBAC (rbac-matrix.md regra 1-2): só ADMIN (0) com permissão ativa
        // altera RBAC/usuários/configurações e acessa o Financeiro.
        Gate::define('admin', fn (Login $login) => UserPermission::where('id_user', $login->id_user)
            ->where('active', true)
            ->where('role', Role::ADMIN->value)
            ->exists());

        // Dashboard (rbac-matrix.md linha Dashboard): resumo p/ roles 0–3;
        // Recrutando (4) ou sem papel ativo → 403 (regra única em DashboardPolicy).
        Gate::define('ver-resumo', function (Login $login): bool {
            $role = app(AuthContract::class)->roleOf((int) $login->id_user);

            return DashboardPolicy::podeVerResumo($role);
        });

        // RH consome o RFID do Auth (fila `database`, sem broker).
        Event::listen(RfidAccessEvent::class, ProcessarPontoRfid::class);

        // Estoque consome a produção concluída (fila `database`, sem broker).
        Event::listen(ProducaoConcluidaEvent::class, ConsumoProducaoListener::class);

        // Vendas consome o status da produção (fila `database`, sem broker; produtor em M6).
        Event::listen(ProducaoStatusAlteradoEvent::class, ProducaoStatusListener::class);

        // Financeiro consome encomenda criada (Vendas) + horas validadas (RH)
        // + produção concluída (produtor real em M6). Listeners nunca propagam
        // falha (warn/ignore), para não quebrar os produtores em fila sync.
        Event::listen(EncomendaCriadaEvent::class, EncomendaCriadaListener::class);
        Event::listen(HorasValidadasEvent::class, HorasValidadasListener::class);
        Event::listen(ProducaoConcluidaEvent::class, ProducaoConcluidaListener::class);

        // Producao consome encomenda criada (Vendas → auto-insert FILA) e nível
        // alterado (RH → trilha de auditoria). Fila `database`, sem broker.
        Event::listen(EncomendaCriadaEvent::class, ProducaoEncomendaCriadaListener::class);
        Event::listen(NivelAlteradoEvent::class, NivelAlteradoListener::class);

        // Notification consome 18 nomes (19 binds por classe do produtor) dos 6
        // módulos (fila `database`, sem broker). Listeners nunca propagam falha
        // (warn/ignore), para não quebrar os produtores em fila sync.
        // `access.rfid` FORA de escopo (sem destinatário — D10); `custo.calculado`
        // (Financeiro-interno) e o mirror `Vendas\ProducaoStatusAlteradoEvent`
        // (consumidor do próprio Vendas) também ficam sem bind.
        Event::listen(NivelAlteradoEvent::class, [RhNotificacaoListener::class, 'handleNivelAlterado']);
        Event::listen(HorasValidadasEvent::class, [RhNotificacaoListener::class, 'handleHorasValidadas']);
        Event::listen(ExtratoMensalHorasEvent::class, [RhNotificacaoListener::class, 'handleExtratoMensal']);
        Event::listen(CertificadoSolicitadoEvent::class, [RhNotificacaoListener::class, 'handleCertificadoSolicitado']);
        Event::listen(CertificadoAprovadoEvent::class, [RhNotificacaoListener::class, 'handleCertificadoAprovado']);
        Event::listen(CertificadoRejeitadoEvent::class, [RhNotificacaoListener::class, 'handleCertificadoRejeitado']);
        Event::listen(EstoqueBaixoEvent::class, [EstoqueNotificacaoListener::class, 'handleEstoqueBaixo']);
        Event::listen(EmprestimoAtrasadoEvent::class, [EstoqueNotificacaoListener::class, 'handleEmprestimoAtrasado']);
        Event::listen(CompraSolicitadaEvent::class, [EstoqueNotificacaoListener::class, 'handleCompraSolicitada']);
        Event::listen(EncomendaCriadaEvent::class, [VendasNotificacaoListener::class, 'handleEncomendaCriada']);
        Event::listen(EncomendaStatusAlteradoEvent::class, [VendasNotificacaoListener::class, 'handleEncomendaStatusAlterado']);
        Event::listen(OrcamentoAprovadoEvent::class, [VendasNotificacaoListener::class, 'handleOrcamentoAprovado']);
        Event::listen(LancamentoVencidoEvent::class, [FinanceiroNotificacaoListener::class, 'handleLancamentoVencido']);
        Event::listen(CompraSolicitadaEvent::class, [FinanceiroNotificacaoListener::class, 'handleCompraSolicitada']);
        Event::listen(KanbanStatusAlteradoEvent::class, [ProducaoNotificacaoListener::class, 'handleKanbanStatusAlterado']);
        Event::listen(ProducaoStatusAlteradoEvent::class, [ProducaoNotificacaoListener::class, 'handleProducaoStatusAlterado']);
        Event::listen(ProducaoConcluidaEvent::class, [ProducaoNotificacaoListener::class, 'handleProducaoConcluida']);
        Event::listen(AdvertenciaRegistradaEvent::class, [ProducaoNotificacaoListener::class, 'handleAdvertenciaRegistrada']);
        Event::listen(AdvertenciaLimiteAtingidoEvent::class, [ProducaoNotificacaoListener::class, 'handleAdvertenciaLimiteAtingido']);
        Event::listen(ProjetoMesaAbandonadoEvent::class, [ProducaoNotificacaoListener::class, 'handleProjetoMesaAbandonado']);
    }
}
