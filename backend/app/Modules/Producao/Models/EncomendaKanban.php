<?php

namespace App\Modules\Producao\Models;

use App\Modules\Producao\Enums\KanbanStatus;
use Illuminate\Database\Eloquent\Model;

/**
 * Cartão do Kanban (`id_encomenda` UNIQUE, LINKA a encomenda do Vendas sem
 * duplicar). `version` = lock otimista manual (409 em conflito).
 */
class EncomendaKanban extends Model
{
    protected $table = 'producao.encomenda_kanban';

    protected $primaryKey = 'id_kanban';

    public $timestamps = false;

    protected $fillable = [
        'id_encomenda',
        'status',
        'data_entrada_status',
        'id_responsavel',
        'ordem',
        'version',
    ];

    protected $casts = [
        'status' => KanbanStatus::class,
        'data_entrada_status' => 'datetime',
        'ordem' => 'integer',
        'version' => 'integer',
    ];
}
