<?php

namespace App\Modules\Vendas\Enums;

/** Alvo da solicitação de edição (ck_solicitacao_alvo_tipo). */
enum TipoAlvoSolicitacao: string
{
    case CLIENTE = 'CLIENTE';
    case ORCAMENTO = 'ORCAMENTO';
    case ENCOMENDA = 'ENCOMENDA';
}
