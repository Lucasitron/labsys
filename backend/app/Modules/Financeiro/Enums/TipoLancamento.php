<?php

namespace App\Modules\Financeiro\Enums;

enum TipoLancamento: string
{
    case ENTRADA = 'ENTRADA';
    case SAIDA = 'SAIDA';
}
