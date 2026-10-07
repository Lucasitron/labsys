<?php

namespace App\Modules\Notification;

use App\Modules\Auth\Contracts\AuthContract;
use App\Modules\Auth\Enums\Role;
use App\Modules\Auth\Models\Login;

/**
 * Identidade do chamador no Notification (o guard JWT do Auth valida; aqui só
 * se resolve papel a partir do Login). Inbox é de qualquer autenticado,
 * inclusive Recrutando (matriz: `Minhas notificações ✅` p/ 0–4).
 */
final class NotificationPrincipal
{
    public function __construct(
        public readonly int $idUser,
        public readonly ?Role $role,
        public readonly bool $admin,
    ) {}

    public static function from(Login $login): self
    {
        $role = app(AuthContract::class)->roleOf((int) $login->id_user);

        return new self(
            idUser: (int) $login->id_user,
            role: $role,
            admin: $role === Role::ADMIN,
        );
    }

    public function isAdmin(): bool
    {
        return $this->admin;
    }
}
