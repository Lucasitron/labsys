<?php

namespace App\Modules\Notification\Enums;

/** Status de ENTREGA da notificação (docs/07 §4 — `lida` governa a leitura). */
enum StatusEntrega: string
{
    case PENDENTE = 'PENDENTE';
    case ENVIADA = 'ENVIADA';
}
