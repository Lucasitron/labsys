<?php

namespace App\Modules\Estoque\Enums;

/** Status do empréstimo (ck_emprestimo_status). */
enum StatusEmprestimo: string
{
    case ATIVO = 'ATIVO';
    case DEVOLVIDO = 'DEVOLVIDO';
    case ATRASADO = 'ATRASADO';
}
