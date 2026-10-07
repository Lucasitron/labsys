<?php

namespace App\Modules\Financeiro\Models;

use Illuminate\Database\Eloquent\Model;

/** Custo congelado da encomenda (Job Order Costing). */
class CustoEncomenda extends Model
{
    protected $table = 'financeiro.custo_encomenda';

    protected $primaryKey = 'id_custo';

    public $timestamps = false;

    protected $fillable = [
        'id_encomenda',
        'custo_materiais',
        'custo_mao_obra',
        'custo_overhead',
        'custo_total',
        'valor_venda',
        'margem_lucro',
        'data_calculo',
    ];

    protected $casts = [
        'id_encomenda' => 'integer',
        'custo_materiais' => 'decimal:2',
        'custo_mao_obra' => 'decimal:2',
        'custo_overhead' => 'decimal:2',
        'custo_total' => 'decimal:2',
        'valor_venda' => 'decimal:2',
        'margem_lucro' => 'decimal:2',
        'data_calculo' => 'date',
    ];
}
