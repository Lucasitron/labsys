<?php

namespace App\Shared\Auth;

use App\Modules\Auth\Contracts\AuthContract;
use App\Modules\Auth\Enums\Role;
use App\Modules\Auth\Models\Login;

class Caller
{
    public readonly int $idPessoa;

    public function __construct(
        public readonly int $idUser,
        public readonly ?Role $role,
        public readonly bool $admin,
    ) {
        $this->idPessoa = $idUser;
    }

    public static function from(Login $login): self
    {
        $role = app(AuthContract::class)->roleOf((int) $login->id_user);

        return new self((int) $login->id_user, $role, $role === Role::ADMIN);
    }

    public function isAdmin(): bool
    {
        return $this->admin;
    }
}
