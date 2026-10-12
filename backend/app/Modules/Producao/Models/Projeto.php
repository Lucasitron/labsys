<?php

namespace App\Modules\Producao\Models;

use App\Modules\Producao\Enums\ProjetoStatus;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\HasMany;

/** Projeto (producao.projeto). */
class Projeto extends Model
{
    protected $table = 'producao.projeto';

    protected $primaryKey = 'id_projeto';

    public $timestamps = false;

    protected $fillable = [
        'nome',
        'descricao',
        'data_inicio',
        'data_fim_prevista',
        'data_fim_real',
        'status',
        'id_responsavel',
    ];

    protected $casts = [
        'data_inicio' => 'date',
        'data_fim_prevista' => 'date',
        'data_fim_real' => 'date',
        'status' => ProjetoStatus::class,
    ];

    public function tarefas(): HasMany
    {
        return $this->hasMany(Tarefa::class, 'id_projeto', 'id_projeto');
    }
}
