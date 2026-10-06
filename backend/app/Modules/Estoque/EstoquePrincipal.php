<?php

namespace App\Modules\Estoque;

use App\Modules\Auth\Contracts\AuthContract;
use App\Modules\Auth\Enums\Role;
use App\Modules\Auth\Models\Login;

/**
 * Identidade do chamador no Estoque (port de EstoquePrincipal: o guard JWT do
 * Auth valida; aqui só se resolve papel/vínculo a partir do Login).
 *
 * Vínculo "responsável por estoque" (rbac-matrix.md regra 5): sem tabela
 * `usuario_responsabilidade` no contrato, o vínculo é o `setor` do login
 * (ESTOQUE/ALMOXARIFADO/SUPRIMENTOS) — server-side, nunca no front.
 */
final class EstoquePrincipal
{
    /** @var list<string> */
    private const SETORES_ESTOQUE = ['ESTOQUE', 'ALMOXARIFADO', 'SUPRIMENTOS'];

    public function __construct(
        public readonly int $idPessoa,
        public readonly ?Role $role,
        public readonly bool $admin,
        public readonly bool $responsavelEstoque,
    ) {}

    public static function from(Login $login): self
    {
        $role = app(AuthContract::class)->roleOf((int) $login->id_user);

        $setor = mb_strtoupper(trim((string) ($login->setor ?? '')));

        return new self(
            idPessoa: (int) $login->id_user,
            role: $role,
            admin: $role === Role::ADMIN,
            responsavelEstoque: in_array($setor, self::SETORES_ESTOQUE, true),
        );
    }

    public function isAdmin(): bool
    {
        return $this->admin;
    }
}
