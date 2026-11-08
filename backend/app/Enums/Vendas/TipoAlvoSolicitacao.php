<?php

namespace App\Enums\Vendas;

/** Alvo da solicitação de edição (ck_solicitacao_alvo_tipo). */
enum TipoAlvoSolicitacao: string
{
    case CLIENTE = 'CLIENTE';
    case ORCAMENTO = 'ORCAMENTO';
    case ENCOMENDA = 'ENCOMENDA';
}
