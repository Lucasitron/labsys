<?php

namespace App\Modules\Producao\Models;

use App\Modules\Producao\Enums\KanbanStatus;
use Illuminate\Database\Eloquent\Model;

/** Trilha de movimentações do Kanban (producao.historico_kanban). */
class HistoricoKanban extends Model
{
    protected $table = 'producao.historico_kanban';

    protected $primaryKey = 'id_historico';

    public $timestamps = false;

    protected $fillable = [
        'id_encomenda',
        'status_anterior',
        'status_novo',
        'data_alteracao',
        'id_usuario',
        'observacao',
    ];

    protected $casts = [
        'status_anterior' => KanbanStatus::class,
        'status_novo' => KanbanStatus::class,
        'data_alteracao' => 'datetime',
    ];
}
