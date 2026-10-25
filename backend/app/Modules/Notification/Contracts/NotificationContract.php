<?php

namespace App\Modules\Notification\Contracts;

/**
 * Fronteira pública do Notification p/ Dashboard (M8, agregador in-process,
 * sem HTTP). Congelado em 2 métodos — só o consumido.
 */
interface NotificationContract
{
    /** Não-lidas do usuário (sino/badge). */
    public function naoLidas(int $idUsuario): int;

    /**
     * Inbox paginada (1-based, `size` 1..100).
     *
     * @return array{items:\Illuminate\Database\Eloquent\Collection<int,\App\Modules\Notification\Models\Notificacao>,total:int,page:int,size:int,pageSize:int,totalPages:int}
     */
    public function inbox(int $idUsuario, int $page, int $size, ?string $tipo = null, ?bool $lida = null): array;
}
