<?php

namespace App\Modules\Rh\Enums;

/** Tipo do certificado de horas. */
enum TipoCertificado: string
{
    case EXTENSAO = 'EXTENSAO';
    case COMPLEMENTAR = 'COMPLEMENTAR';
    case ESTAGIO = 'ESTAGIO';
}
