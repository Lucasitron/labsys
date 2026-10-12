<?php

namespace App\Modules\Producao\Enums;

/** Prioridade de uma tarefa. */
enum PrioridadeTarefa: string
{
    case BAIXA = 'BAIXA';
    case MEDIA = 'MEDIA';
    case ALTA = 'ALTA';
}
