<?php

namespace App\Modules\Vendas\Models;

use Illuminate\Database\Eloquent\Model;

/** Item do orçamento (vendas.item_orcamento). Sem id_item_estoque persistido (validação transitória). */
class ItemOrcamento extends Model
{
    protected $table = 'vendas.item_orcamento';

    protected $primaryKey = 'id_item_orcamento';

    public $timestamps = false;

    protected $fillable = [
        'id_orcamento',
        'descricao',
        'quantidade',
        'valor_unitario',
        'material_tipo',
        'material_quantidade',
        'material_unidade',
        'horas',
        'compra',
    ];

    protected $casts = [
        'quantidade' => 'decimal:2',
        'valor_unitario' => 'decimal:2',
        'material_quantidade' => 'decimal:3',
        'horas' => 'decimal:2',
        'compra' => 'boolean',
    ];
}
