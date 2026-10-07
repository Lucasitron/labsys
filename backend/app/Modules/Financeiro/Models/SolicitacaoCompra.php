<?php

namespace App\Modules\Financeiro\Models;

use App\Modules\Financeiro\Enums\StatusCompra;
use Illuminate\Database\Eloquent\Model;

/** Solicitação de compra (fluxo informativo, sem bloqueio). */
class SolicitacaoCompra extends Model
{
    protected $table = 'financeiro.solicitacao_compra';

    protected $primaryKey = 'id_solicitacao';

    public $timestamps = false;

    protected $fillable = [
        'id_item_estoque',
        'quantidade',
        'valor_estimado',
        'status',
        'data_solicitacao',
    ];

    protected $casts = [
        'id_item_estoque' => 'integer',
        'quantidade' => 'decimal:2',
        'valor_estimado' => 'decimal:2',
        'status' => StatusCompra::class,
        'data_solicitacao' => 'date',
    ];
}
