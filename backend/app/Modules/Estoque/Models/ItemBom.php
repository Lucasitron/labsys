<?php

namespace App\Modules\Estoque\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/** Item da BOM (estoque.item_bom). */
class ItemBom extends Model
{
    protected $table = 'estoque.item_bom';

    protected $primaryKey = 'id_item_bom';

    public $timestamps = false;

    protected $fillable = ['id_bom', 'id_item', 'quantidade_prevista', 'quantidade_real'];

    protected $casts = [
        'quantidade_prevista' => 'decimal:2',
        'quantidade_real' => 'decimal:2',
    ];

    public function bom(): BelongsTo
    {
        return $this->belongsTo(ListaMateriais::class, 'id_bom', 'id_bom');
    }

    public function item(): BelongsTo
    {
        return $this->belongsTo(Item::class, 'id_item', 'id_item');
    }
}
