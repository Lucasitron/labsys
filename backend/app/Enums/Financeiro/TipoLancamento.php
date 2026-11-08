<?php

namespace App\Enums\Financeiro;

enum TipoLancamento: string
{
    case ENTRADA = 'ENTRADA';
    case SAIDA = 'SAIDA';
}
