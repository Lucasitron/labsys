<?php

namespace App\Modules\Rh\Enums;

/** Status do apontamento de horas. Nasce PENDENTE; Admin/tutor valida ou rejeita. */
enum StatusApontamento: string
{
    case PENDENTE = 'PENDENTE';
    case VALIDADO = 'VALIDADO';
    case REJEITADO = 'REJEITADO';
}
