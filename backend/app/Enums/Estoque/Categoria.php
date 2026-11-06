<?php

namespace App\Enums\Estoque;

/** Categoria do item (ck_item_categoria). */
enum Categoria: string
{
    case INSUMO = 'INSUMO';
    case FERRAMENTA = 'FERRAMENTA';
    case PECA = 'PECA';
}
