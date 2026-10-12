<?php

namespace App\Modules\Vendas\Models;

use Illuminate\Database\Eloquent\Model;

/** Tarefa interna de marketing (vendas.tarefa_marketing). Status/prioridade via `in:` (sem enum). */
class TarefaMarketing extends Model
{
    protected $table = 'vendas.tarefa_marketing';

    protected $primaryKey = 'id_tarefa';

    public $timestamps = false;

    protected $fillable = [
        'titulo',
        'descricao',
        'id_responsavel',
        'data_inicio',
        'data_fim',
        'status',
        'prioridade',
        'criado_por',
    ];

    protected $casts = [
        'data_inicio' => 'date',
        'data_fim' => 'date',
    ];
}
