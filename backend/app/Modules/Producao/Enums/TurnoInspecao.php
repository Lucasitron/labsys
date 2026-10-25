<?php

namespace App\Modules\Producao\Enums;

/** Turno de uma inspeção 5S. */
enum TurnoInspecao: string
{
    case MANHA = 'MANHA';
    case TARDE = 'TARDE';
}
