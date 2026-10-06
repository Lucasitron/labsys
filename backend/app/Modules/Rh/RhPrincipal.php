<?php

namespace App\Modules\Rh;

use App\Modules\Auth\Contracts\AuthContract;
use App\Modules\Auth\Enums\Role;
use App\Modules\Auth\Models\Login;
use App\Modules\Rh\Enums\NivelAcesso;
use App\Modules\Rh\Models\Funcionario;

/**
 * Identidade do chamador no RH (port de RhPrincipal: o guard JWT do Auth
 * valida; aqui só se resolve pessoa/vínculo/nível a partir do Login).
 *
 * Sem vínculo de funcionário, escopos "próprios" degradam para Admin-only:
 * não-Admin sem vínculo recebe 403 nos services (via RhPolicy).
 */
final class RhPrincipal
{
    public function __construct(
        public readonly int $idPessoa,
        public readonly ?int $idFuncionario,
        public readonly ?NivelAcesso $nivel,
        public readonly bool $admin,
    ) {}

    public static function from(Login $login): self
    {
        $funcionario = Funcionario::where('id_pessoa', $login->id_user)->first();

        // Papel via fronteira do Auth (nunca ler user_permissions direto).
        $role = app(AuthContract::class)->roleOf((int) $login->id_user);

        $nivel = $role !== null
            ? NivelAcesso::from($role->value)
            : $funcionario?->nivel_acesso;

        return new self(
            idPessoa: (int) $login->id_user,
            idFuncionario: $funcionario !== null ? (int) $funcionario->getKey() : null,
            nivel: $nivel,
            admin: $role === Role::ADMIN,
        );
    }

    public function isAdmin(): bool
    {
        return $this->admin;
    }
}
