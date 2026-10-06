<?php

namespace App\Providers;

use App\Modules\Auth\Contracts\AuthContract;
use App\Modules\Auth\Contracts\DefaultAuthContract;
use App\Modules\Auth\Enums\Role;
use App\Modules\Auth\Models\Login;
use App\Modules\Auth\Models\UserPermission;
use Illuminate\Http\Resources\Json\JsonResource;
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
    }

    /**
     * Bootstrap any application services.
     */
    public function boot(): void
    {
        // Recursos de API sem envelope "data" (contrato direto com o front).
        JsonResource::withoutWrapping();

        // Migrações por módulo vivem em database/migrations/<modulo>/ e são
        // descobertas automaticamente (scan recursivo do migrator).

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
    }
}
