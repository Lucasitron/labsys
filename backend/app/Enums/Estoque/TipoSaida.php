<?php

namespace App\Enums\Estoque;

/** Tipo da saída (ck_saida_tipo). */
enum TipoSaida: string
{
    case CONSUMO = 'CONSUMO';
    case PERDA = 'PERDA';
    case AJUSTE = 'AJUSTE';
    case EMPRESTIMO = 'EMPRESTIMO';
}
