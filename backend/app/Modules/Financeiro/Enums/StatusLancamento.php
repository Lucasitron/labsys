<?php

namespace App\Modules\Financeiro\Enums;

enum StatusLancamento: string
{
    case PENDENTE = 'PENDENTE';
    case PAGO = 'PAGO';
    case ATRASADO = 'ATRASADO';
    case CANCELADO = 'CANCELADO';
}
