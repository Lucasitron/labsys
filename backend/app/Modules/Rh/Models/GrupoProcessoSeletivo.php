<?php

namespace App\Modules\Rh\Models;

use App\Modules\Rh\Enums\StatusProcesso;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/** Grupo de candidatos do PS, liderado por um tutor. */
class GrupoProcessoSeletivo extends Model
{
    protected $table = 'rh.grupo_processo_seletivo';

    public $timestamps = false;

    protected $fillable = ['nome', 'id_tutor_lider', 'etapa'];

    protected $casts = [
        'etapa' => StatusProcesso::class,
    ];

    public function lider(): BelongsTo
    {
        return $this->belongsTo(Funcionario::class, 'id_tutor_lider');
    }
}
