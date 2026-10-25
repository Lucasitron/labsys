<?php

namespace App\Modules\Producao\Enums;

/** Situação de uma tarefa. */
enum TarefaStatus: string
{
    case PENDENTE = 'PENDENTE';
    case EM_ANDAMENTO = 'EM_ANDAMENTO';
    case CONCLUIDA = 'CONCLUIDA';
    case ATRASADA = 'ATRASADA';
}
