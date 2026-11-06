<?php

namespace App\Services\Estoque;

use App\Contracts\AuthContract;
use App\Enums\Auth\Role;
use App\Models\Auth\Login;
use App\Shared\Auth\Caller;

/**
 * Identidade do chamador no Estoque (port de EstoquePrincipal: o guard JWT do
 * Auth valida; aqui só se resolve papel/vínculo a partir do Login).
 *
 * Vínculo "responsável por estoque" (rbac-matrix.md regra 5): sem tabela
 * `usuario_responsabilidade` no contrato, o vínculo é o `setor` do login
 * (ESTOQUE/ALMOXARIFADO/SUPRIMENTOS) — server-side, nunca no front.
 */
class EstoquePrincipal extends Caller
{
    /** @var list<string> */
    private const SETORES_ESTOQUE = ['ESTOQUE', 'ALMOXARIFADO', 'SUPRIMENTOS'];

    public function __construct(
        int $idPessoa,
        ?Role $role,
        bool $admin,
        public readonly bool $responsavelEstoque,
    ) {
        parent::__construct($idPessoa, $role, $admin);
    }

    public static function from(Login $login): self
    {
        $caller = Caller::from($login);

        $setor = mb_strtoupper(trim((string) ($login->setor ?? '')));

        return new self(
            idPessoa: $caller->idUser,
            role: $caller->role,
            admin: $caller->admin,
            responsavelEstoque: in_array($setor, self::SETORES_ESTOQUE, true),
        );
    }
}
