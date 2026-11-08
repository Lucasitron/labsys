<?php

namespace App\Enums\Producao;

/** Situação de um projeto (nomes do contrato Java, maiúsculos). */
enum ProjetoStatus: string
{
    case PLANEJADO = 'PLANEJADO';
    case EM_ANDAMENTO = 'EM_ANDAMENTO';
    case CONCLUIDO = 'CONCLUIDO';
    case CANCELADO = 'CANCELADO';
}
