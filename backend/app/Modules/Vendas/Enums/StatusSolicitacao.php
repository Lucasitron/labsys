<?php

namespace App\Modules\Vendas\Enums;

/** Status da solicitação de edição (ck_solicitacao_status). */
enum StatusSolicitacao: string
{
    case PENDENTE = 'Pendente';
    case APROVADA = 'Aprovada';
    case REJEITADA = 'Rejeitada';
}
