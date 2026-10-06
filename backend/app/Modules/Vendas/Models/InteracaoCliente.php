<?php

namespace App\Modules\Vendas\Models;

use Illuminate\Database\Eloquent\Model;

/** Interação do CRM (vendas.interacao_cliente). Responsável = id, sem join cross-schema. */
class InteracaoCliente extends Model
{
    protected $table = 'vendas.interacao_cliente';

    protected $primaryKey = 'id_interacao';

    public $timestamps = false;

    protected $fillable = [
        'id_cliente',
        'data_interacao',
        'tipo',
        'descricao',
        'id_usuario',
    ];

    protected $casts = [
        'data_interacao' => 'datetime',
    ];
}
