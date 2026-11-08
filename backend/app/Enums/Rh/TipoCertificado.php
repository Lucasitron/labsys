<?php

namespace App\Enums\Rh;

/** Tipo do certificado de horas. */
enum TipoCertificado: string
{
    case EXTENSAO = 'EXTENSAO';
    case COMPLEMENTAR = 'COMPLEMENTAR';
    case ESTAGIO = 'ESTAGIO';
}
