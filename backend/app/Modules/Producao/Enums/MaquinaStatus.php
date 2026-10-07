<?php

namespace App\Modules\Producao\Enums;

/** Situação operacional de uma máquina. */
enum MaquinaStatus: string
{
    case DISPONIVEL = 'DISPONIVEL';
    case EM_USO = 'EM_USO';
    case MANUTENCAO = 'MANUTENCAO';
}
