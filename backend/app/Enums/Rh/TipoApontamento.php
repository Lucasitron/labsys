<?php

namespace App\Enums\Rh;

/** Tipo do apontamento de horas. */
enum TipoApontamento: string
{
    case ENCOMENDA = 'ENCOMENDA';
    case PROJETO = 'PROJETO';
}
