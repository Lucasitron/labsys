<?php

namespace App\Contracts;

use App\Models\Notification\Notificacao;
use Illuminate\Database\Eloquent\Collection;

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
     * @return array{items:Collection<int,Notificacao>,total:int,page:int,size:int,pageSize:int,totalPages:int}
     */
    public function inbox(int $idUsuario, int $page, int $size, ?string $tipo = null, ?bool $lida = null): array;
}
