<?php

namespace App\Enums\Vendas;

/** Status do orçamento (ck_orcamento_status). */
enum StatusOrcamento: string
{
    case PENDENTE = 'Pendente';
    case APROVADO = 'Aprovado';
    case RECUSADO = 'Recusado';
    case AJUSTE = 'Ajuste';
}
