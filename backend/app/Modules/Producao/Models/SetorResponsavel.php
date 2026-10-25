<?php

namespace App\Modules\Producao\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/** Responsável do setor com rotação (producao.setor_responsavel). Ids opacos, sem join cross-schema. */
class SetorResponsavel extends Model
{
    protected $table = 'producao.setor_responsavel';

    protected $primaryKey = 'id_responsavel';

    public $timestamps = false;

    protected $fillable = [
        'id_setor',
        'id_funcionario',
        'data_inicio',
        'data_fim',
        'ativo',
    ];

    protected $casts = [
        'data_inicio' => 'date',
        'data_fim' => 'date',
        'ativo' => 'boolean',
    ];

    public function setor(): BelongsTo
    {
        return $this->belongsTo(Setor::class, 'id_setor', 'id_setor');
    }
}
