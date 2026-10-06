<?php

namespace App\Modules\Vendas\Enums;

/** Tipo de alteração pedida (ck_solicitacao_tipo). */
enum TipoSolicitacao: string
{
    case ALTERACAO_DADOS = 'ALTERACAO_DADOS';
    case MUDANCA_STATUS = 'MUDANCA_STATUS';
    case MOVER_ENCOMENDA = 'MOVER_ENCOMENDA';
    case OUTRA = 'OUTRA';
}
