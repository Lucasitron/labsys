<?php

namespace App\Modules\Producao\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/** Sinalização impressa do setor (producao.setor_sinalizacao). */
class SetorSinalizacao extends Model
{
    protected $table = 'producao.setor_sinalizacao';

    protected $primaryKey = 'id_sinalizacao';

    public $timestamps = false;

    protected $fillable = [
        'id_setor',
        'texto',
    ];

    public function setor(): BelongsTo
    {
        return $this->belongsTo(Setor::class, 'id_setor', 'id_setor');
    }
}
