<?php

namespace App\Enums\Auth;

/** Tipo do registro de acesso físico (access_log.type). */
enum AccessLogType: string
{
    case ENTRADA = 'ENTRADA';
    case SAIDA = 'SAIDA';
    case ACESSO_NEGADO = 'ACESSO_NEGADO';
}
