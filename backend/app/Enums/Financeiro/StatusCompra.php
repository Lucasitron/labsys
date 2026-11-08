<?php

namespace App\Enums\Financeiro;

enum StatusCompra: string
{
    case REGISTRADA = 'REGISTRADA';
    case VISUALIZADA = 'VISUALIZADA';
    case CONCLUIDA = 'CONCLUIDA';
}
