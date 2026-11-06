<?php

namespace App\Contracts;

use App\Services\Notification\NotificacaoService;

class DefaultNotificationContract implements NotificationContract
{
    public function __construct(private NotificacaoService $inbox) {}

    public function naoLidas(int $idUsuario): int
    {
        return $this->inbox->contarNaoLidas($idUsuario);
    }

    public function inbox(int $idUsuario, int $page, int $size, ?string $tipo = null, ?bool $lida = null): array
    {
        return $this->inbox->listar($idUsuario, $page, $size, $tipo, $lida);
    }
}
