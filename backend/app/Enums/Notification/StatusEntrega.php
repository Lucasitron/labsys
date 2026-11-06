<?php

namespace App\Enums\Notification;

/** Status de ENTREGA da notificação (docs/07 §4 — `lida` governa a leitura). */
enum StatusEntrega: string
{
    case PENDENTE = 'PENDENTE';
    case ENVIADA = 'ENVIADA';
}
