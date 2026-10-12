<?php

namespace App\Modules\Rh\Enums;

/** Status da solicitação de certificado. */
enum StatusSolicitacao: string
{
    case PENDENTE = 'PENDENTE';
    case APROVADO = 'APROVADO';
    case REJEITADO = 'REJEITADO';
}
