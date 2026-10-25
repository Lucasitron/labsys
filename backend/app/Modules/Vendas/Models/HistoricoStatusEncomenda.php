<?php

namespace App\Modules\Vendas\Models;

use Illuminate\Database\Eloquent\Model;

/** Auditoria do Kanban (vendas.historico_status_encomenda). Leitura dobrada no EncomendaService. */
class HistoricoStatusEncomenda extends Model
{
    protected $table = 'vendas.historico_status_encomenda';

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
        'data_alteracao' => 'datetime',
    ];
}
