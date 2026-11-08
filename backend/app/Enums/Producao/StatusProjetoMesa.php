<?php

namespace App\Enums\Producao;

/** Situação de um projeto de mesa individual. */
enum StatusProjetoMesa: string
{
    case ATIVO = 'ATIVO';
    case ABANDONADO = 'ABANDONADO';
    case CONCLUIDO = 'CONCLUIDO';
}
