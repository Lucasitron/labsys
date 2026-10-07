<?php

namespace App\Modules\Producao\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

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

    public function inspecao(): BelongsTo
    {
        return $this->belongsTo(Inspecao5S::class, 'id_inspecao', 'id_inspecao');
    }

    public function checklist(): BelongsTo
    {
        return $this->belongsTo(SetorChecklist::class, 'id_checklist', 'id_checklist');
    }
}
