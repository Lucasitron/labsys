<?php

namespace App\Modules\Producao\Models;

use Illuminate\Database\Eloquent\Model;

/** Item avaliado na inspeção (producao.item_inspecao_5s). */
class ItemInspecao5S extends Model
{
    protected $table = 'producao.item_inspecao_5s';

    protected $primaryKey = 'id_item_inspecao';

    public $timestamps = false;

    protected $fillable = [
        'id_inspecao',
        'id_checklist',
        'conforme',
        'observacao',
    ];

    protected $casts = [
        'conforme' => 'boolean',
    ];

}
