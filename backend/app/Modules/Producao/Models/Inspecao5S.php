<?php

namespace App\Modules\Producao\Models;

use App\Modules\Producao\Enums\StatusInspecao;
use App\Modules\Producao\Enums\TurnoInspecao;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasMany;

/** Inspeção 5S (producao.inspecao_5s). Status recalculado no backend. */
class Inspecao5S extends Model
{
    protected $table = 'producao.inspecao_5s';

    protected $primaryKey = 'id_inspecao';

    public $timestamps = false;

    protected $fillable = [
        'id_setor',
        'id_inspetor',
        'data_inspecao',
        'turno',
        'status',
        'observacoes',
    ];

    protected $casts = [
        'data_inspecao' => 'date',
        'turno' => TurnoInspecao::class,
        'status' => StatusInspecao::class,
    ];

    public function setor(): BelongsTo
    {
        return $this->belongsTo(Setor::class, 'id_setor', 'id_setor');
    }

    public function itens(): HasMany
    {
        return $this->hasMany(ItemInspecao5S::class, 'id_inspecao', 'id_inspecao');
    }

    /** Qualquer item não-conforme → NAO_CONFORME (equivale a `recalcularStatus`). */
    public function recalculado(): StatusInspecao
    {
        foreach ($this->itens as $item) {
            if ($item->conforme === false) {
                return StatusInspecao::NAO_CONFORME;
            }
        }

        return StatusInspecao::OK;
    }
}
