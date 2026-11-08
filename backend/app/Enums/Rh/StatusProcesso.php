<?php

namespace App\Enums\Rh;

/** Etapa do candidato no processo seletivo. */
enum StatusProcesso: string
{
    case INSCRITO = 'INSCRITO';
    case EM_TRIAGEM = 'EM_TRIAGEM';
    case ENTREVISTA = 'ENTREVISTA';
    case APROVADO = 'APROVADO';
    case REPROVADO = 'REPROVADO';
}
