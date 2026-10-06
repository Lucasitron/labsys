<?php

namespace App\Modules\Estoque\Enums;

/** Categoria do item (ck_item_categoria). */
enum Categoria: string
{
    case INSUMO = 'INSUMO';
    case FERRAMENTA = 'FERRAMENTA';
    case PECA = 'PECA';
}
