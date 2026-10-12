<?php

namespace App\Modules\Vendas;

use App\Modules\Auth\Contracts\AuthContract;
use App\Modules\Auth\Enums\Role;
use App\Modules\Auth\Models\Login;

/**
 * Identidade do chamador no Vendas (o guard JWT do Auth valida; aqui só se
 * resolve papel a partir do Login). Sem "responsável de Vendas" (decisão PO:
 * Admin decide; Kanban usa criador+Admin na Policy).
 */
final class VendasPrincipal
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
