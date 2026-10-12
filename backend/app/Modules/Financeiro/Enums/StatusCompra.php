<?php

namespace App\Modules\Financeiro\Enums;

enum StatusCompra: string
{
    case REGISTRADA = 'REGISTRADA';
    case VISUALIZADA = 'VISUALIZADA';
    case CONCLUIDA = 'CONCLUIDA';
}
