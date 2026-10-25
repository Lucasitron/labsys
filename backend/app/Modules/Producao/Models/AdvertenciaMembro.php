<?php

namespace App\Modules\Producao\Models;

use App\Modules\Producao\Enums\TipoAdvertencia;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/** Advertência de membro (producao.advertencia_membro). Contador sequencial por membro. */
class AdvertenciaMembro extends Model
{
    protected $table = 'producao.advertencia_membro';

    protected $primaryKey = 'id_advertencia';

    public $timestamps = false;

    protected $fillable = [
        'id_funcionario',
        'id_inspecao',
        'data',
        'motivo',
        'tipo',
        'contador',
        'id_admin_registrou',
    ];

    protected $casts = [
        'data' => 'date',
        'tipo' => TipoAdvertencia::class,
        'contador' => 'integer',
    ];

    public function inspecao(): BelongsTo
    {
        return $this->belongsTo(Inspecao5S::class, 'id_inspecao', 'id_inspecao');
    }
}
