<?php

namespace App\Modules\Rh\Models;

use App\Modules\Rh\Enums\StatusProcesso;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/** Processo seletivo do candidato (pessoa + funcionário nível 4). */
class ProcessoSeletivo extends Model
{
    protected $table = 'rh.processo_seletivo';

    public $timestamps = false;

    protected $fillable = [
        'id_candidato',
        'id_tutor',
        'status_processo',
        'data_inscricao',
        'resultado_final',
        'id_grupo',
        'nota',
        'feedback',
    ];

    protected $casts = [
        'status_processo' => StatusProcesso::class,
        'data_inscricao' => 'date',
        'nota' => 'decimal:2',
    ];

    public function candidato(): BelongsTo
    {
        return $this->belongsTo(Pessoa::class, 'id_candidato');
    }

    public function tutor(): BelongsTo
    {
        return $this->belongsTo(Funcionario::class, 'id_tutor');
    }

    public function grupo(): BelongsTo
    {
        return $this->belongsTo(GrupoProcessoSeletivo::class, 'id_grupo');
    }
}
