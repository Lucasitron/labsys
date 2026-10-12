<?php

namespace App\Modules\Financeiro\Enums;

enum StatusFechamento: string
{
    case ABERTA = 'ABERTA';
    case CONCLUIDA = 'CONCLUIDA';
    case CANCELADA = 'CANCELADA';
}
