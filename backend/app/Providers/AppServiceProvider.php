<?php

namespace App\Providers;

use App\Modules\Auth\Contracts\AuthContract;
use App\Modules\Auth\Contracts\DefaultAuthContract;
use App\Modules\Auth\Enums\Role;
use App\Modules\Auth\Events\RfidAccessEvent;
use App\Modules\Auth\Models\Login;
use App\Modules\Auth\Models\UserPermission;
use App\Modules\Estoque\Contracts\DefaultEstoqueContract;
use App\Modules\Estoque\Contracts\EstoqueContract;
use App\Modules\Estoque\Events\ProducaoConcluidaEvent;
use App\Modules\Estoque\Listeners\ConsumoProducaoListener;
use App\Modules\Rh\Contracts\DefaultRhContract;
use App\Modules\Rh\Contracts\RhContract;
use App\Modules\Rh\Listeners\ProcessarPontoRfid;
use App\Modules\Vendas\Contracts\DefaultVendasContract;
use App\Modules\Vendas\Contracts\VendasContract;
use App\Modules\Vendas\Events\ProducaoStatusAlteradoEvent;
use App\Modules\Vendas\Listeners\ProducaoStatusListener;
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
        // Fronteira in-process do Auth (único consumo permitido por outros módulos).
        $this->app->singleton(AuthContract::class, DefaultAuthContract::class);

        // Fronteira in-process do RH (Financeiro/Produção consomem só o Contract).
        $this->app->singleton(RhContract::class, DefaultRhContract::class);

        // Fronteira in-process do Estoque (Vendas/M4 e Produção/M6 consomem só o Contract).
        $this->app->singleton(EstoqueContract::class, DefaultEstoqueContract::class);

        // Fronteira in-process do Vendas (Produção/M6 e Financeiro/M5 consomem só o Contract).
        $this->app->singleton(VendasContract::class, DefaultVendasContract::class);
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

        // RH consome o RFID do Auth (fila `database`, sem broker).
        Event::listen(RfidAccessEvent::class, ProcessarPontoRfid::class);

        // Estoque consome a produção concluída (fila `database`, sem broker).
        Event::listen(ProducaoConcluidaEvent::class, ConsumoProducaoListener::class);

        // Vendas consome o status da produção (fila `database`, sem broker; produtor em M6).
        Event::listen(ProducaoStatusAlteradoEvent::class, ProducaoStatusListener::class);
    }
}
