<?php

namespace App\Modules\Producao\Enums;

/** Resultado de uma inspeção 5S (recalculado no backend, nunca no payload). */
enum StatusInspecao: string
{
    case OK = 'OK';
    case NAO_CONFORME = 'NAO_CONFORME';
}
