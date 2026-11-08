<?php

namespace App\Models\Financeiro;

use App\Enums\Financeiro\TipoDoacao;
use Illuminate\Database\Eloquent\Model;

/** Doação ou recurso de projeto (financeiro.doacao_recurso). */
class DoacaoRecurso extends Model
{
    protected $table = 'financeiro.doacao_recurso';

    protected $primaryKey = 'id_doacao';

    public $timestamps = false;

    protected $fillable = [
        'tipo',
        'origem',
        'valor',
        'data_recebimento',
        'id_projeto_associado',
    ];

    protected $casts = [
        'tipo' => TipoDoacao::class,
        'valor' => 'decimal:2',
        'data_recebimento' => 'date',
        'id_projeto_associado' => 'integer',
    ];
}
