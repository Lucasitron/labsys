<?php

namespace App\Modules\Financeiro;

use App\Modules\Auth\Contracts\AuthContract;
use App\Modules\Auth\Enums\Role;
use App\Modules\Auth\Models\Login;

/**
 * Identidade do chamador no Financeiro (o guard JWT do Auth valida; aqui só
 * se resolve o papel a partir do Login). Sem "responsável financeiro" e sem
 * vínculo `usuario_responsabilidade` (matriz: módulo exclusivo Admin).
 */
final class FinanceiroPrincipal
{
    public function __construct(
        public readonly int $idPessoa,
        public readonly ?Role $role,
        public readonly bool $admin,
    ) {}

    public static function from(Login $login): self
    {
        $role = app(AuthContract::class)->roleOf((int) $login->id_user);

        return new self(
            idPessoa: (int) $login->id_user,
            role: $role,
            admin: $role === Role::ADMIN,
        );
    }

    public function isAdmin(): bool
    {
        return $this->admin;
    }
}
