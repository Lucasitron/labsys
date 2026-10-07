<?php

namespace App\Modules\Notification\Policies;

use App\Modules\Notification\Models\Notificacao;
use App\Shared\Exceptions\ForbiddenException;

/**
 * Autorização do Notification (rbac-matrix.md linha Notification).
 *
 * - Inbox (listar/contar/ler/ler-todas): qualquer autenticado, inclusive
 *   Recrutando — sem `exigeLeitura` anti-Recrutando (delta consciente vs os
 *   demais módulos). O isolamento é server-side: o service sempre filtra pelo
 *   `id_user` do guard, nunca por parâmetro.
 * - `marcarComoLida`: dono OU Admin (bypass Java 1:1).
 * - preferences/history/revisar/config: Admin-only (Gate `can:admin` na rota).
 */
class NotificationPolicy
{
    /** Dono OU Admin (bypass Java 1:1) — ponto único, chamado pelo service. */
    public static function exigeDonoOuAdmin(Notificacao $notificacao, int $idUsuario, bool $admin): void
    {
        if ($admin) {
            return;
        }

        if ((int) $notificacao->id_usuario !== $idUsuario) {
            throw new ForbiddenException('Você só pode ler as suas próprias notificações');
        }
    }
}
