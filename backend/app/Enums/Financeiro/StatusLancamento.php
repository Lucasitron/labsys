<?php

namespace App\Enums\Financeiro;

enum StatusLancamento: string
{
    case PENDENTE = 'PENDENTE';
    case PAGO = 'PAGO';
    case ATRASADO = 'ATRASADO';
    case CANCELADO = 'CANCELADO';
}
