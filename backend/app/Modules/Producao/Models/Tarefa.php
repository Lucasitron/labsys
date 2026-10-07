<?php

namespace App\Modules\Producao\Models;

use App\Modules\Producao\Enums\PrioridadeTarefa;
use App\Modules\Producao\Enums\TarefaStatus;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/** Tarefa vinculada a projeto (producao.tarefa). */
class Tarefa extends Model
{
    protected $table = 'producao.tarefa';

    protected $primaryKey = 'id_tarefa';

    public $timestamps = false;

    protected $fillable = [
        'id_projeto',
        'titulo',
        'descricao',
        'id_responsavel',
        'data_inicio',
        'data_fim_prevista',
        'data_conclusao',
        'status',
        'prioridade',
    ];

    protected $casts = [
        'data_inicio' => 'date',
        'data_fim_prevista' => 'date',
        'data_conclusao' => 'date',
        'status' => TarefaStatus::class,
        'prioridade' => PrioridadeTarefa::class,
    ];

    public function projeto(): BelongsTo
    {
        return $this->belongsTo(Projeto::class, 'id_projeto', 'id_projeto');
    }
}
