<?php

namespace App\Modules\Rh;

use App\Modules\Auth\Models\Login;
use App\Modules\Rh\Enums\NivelAcesso;
use App\Modules\Auth\Enums\Role;
use App\Modules\Rh\Models\Funcionario;
use App\Shared\Auth\Caller;

/**
 * Identidade do chamador no RH (port de RhPrincipal: o guard JWT do Auth
 * valida; aqui só se resolve pessoa/vínculo/nível a partir do Login).
 *
 * Sem vínculo de funcionário, escopos "próprios" degradam para Admin-only:
 * não-Admin sem vínculo recebe 403 nos services (via RhPolicy).
 */
class RhPrincipal extends Caller
{
    public function __construct(
        int $idPessoa,
        public readonly ?int $idFuncionario,
        public readonly ?NivelAcesso $nivel,
        bool $admin,
        ?Role $role = null,
    ) {
        parent::__construct($idPessoa, $role ?? ($nivel ? Role::from($nivel->value) : null), $admin);
    }

    public static function from(Login $login): self
    {
        $caller = Caller::from($login);

        $funcionario = Funcionario::where('id_pessoa', $login->id_user)->first();

        $nivel = $caller->role !== null
            ? NivelAcesso::from($caller->role->value)
            : $funcionario?->nivel_acesso;

        return new self(
            idPessoa: (int) $login->id_user,
            idFuncionario: $funcionario !== null ? (int) $funcionario->getKey() : null,
            nivel: $nivel,
            admin: $caller->admin,
            role: $caller->role,
        );
    }
}
