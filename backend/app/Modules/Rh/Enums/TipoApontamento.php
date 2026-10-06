<?php

namespace App\Modules\Rh\Enums;

/** Tipo do apontamento de horas. */
enum TipoApontamento: string
{
    case ENCOMENDA = 'ENCOMENDA';
    case PROJETO = 'PROJETO';
}
