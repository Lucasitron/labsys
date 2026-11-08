<?php

namespace App\Enums\Producao;

/** Prioridade de uma tarefa. */
enum PrioridadeTarefa: string
{
    case BAIXA = 'BAIXA';
    case MEDIA = 'MEDIA';
    case ALTA = 'ALTA';
}
