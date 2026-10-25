<?php

namespace App\Modules\Vendas\Models;

use App\Modules\Vendas\Enums\StatusSolicitacao;
use App\Modules\Vendas\Enums\TipoAlvoSolicitacao;
use App\Modules\Vendas\Enums\TipoSolicitacao;
use Illuminate\Database\Eloquent\Model;

/** Solicitação de edição (vendas.solicitacao_edicao). D-4: decidir só flipa status. */
class SolicitacaoEdicao extends Model
{
    protected $table = 'vendas.solicitacao_edicao';

    protected $primaryKey = 'id_solicitacao';

    public $timestamps = false;

    protected $fillable = [
        'tipo',
        'alvo_tipo',
        'alvo_id',
        'campo',
        'valor_atual',
        'valor_proposto',
        'justificativa',
        'status',
        'solicitante_id',
        'decidido_por',
        'motivo_decisao',
        'data_criacao',
        'data_decisao',
    ];

    protected $casts = [
        'tipo' => TipoSolicitacao::class,
        'alvo_tipo' => TipoAlvoSolicitacao::class,
        'status' => StatusSolicitacao::class,
        'data_criacao' => 'datetime',
        'data_decisao' => 'datetime',
    ];
}
