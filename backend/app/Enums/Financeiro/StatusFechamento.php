<?php

namespace App\Enums\Financeiro;

enum StatusFechamento: string
{
    case ABERTA = 'ABERTA';
    case CONCLUIDA = 'CONCLUIDA';
    case CANCELADA = 'CANCELADA';
}
