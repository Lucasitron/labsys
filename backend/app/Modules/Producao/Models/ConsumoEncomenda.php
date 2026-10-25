<?php

namespace App\Modules\Producao\Models;

use Illuminate\Database\Eloquent\Model;

/** BOM final por encomenda (producao.consumo_encomenda). Upsert por `(id_encomenda,id_item)`. */
class ConsumoEncomenda extends Model
{
    protected $table = 'producao.consumo_encomenda';

    protected $primaryKey = 'id_consumo';

    public $timestamps = false;

    protected $fillable = [
        'id_encomenda',
        'id_item',
        'quantidade_consumida',
    ];

    protected $casts = [
        'quantidade_consumida' => 'decimal:2',
    ];
}
