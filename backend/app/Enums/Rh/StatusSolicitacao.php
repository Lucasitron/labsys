<?php

namespace App\Enums\Rh;

/** Status da solicitação de certificado. */
enum StatusSolicitacao: string
{
    case PENDENTE = 'PENDENTE';
    case APROVADO = 'APROVADO';
    case REJEITADO = 'REJEITADO';
}
