<?php

namespace App\Modules\Financeiro\Models;

use App\Modules\Financeiro\Enums\StatusFechamento;
use Illuminate\Database\Eloquent\Model;

/** Fechamento de encomenda — congela valor/horas (D-5: imutável, nova ordem em vez de edição). */
class FechamentoEncomenda extends Model
{
    protected $table = 'financeiro.fechamento_encomenda';

    protected $primaryKey = 'id_fechamento';

    public $timestamps = false;

    protected $fillable = [
        'id_encomenda',
        'horas_estimadas',
        'valor_fechado',
        'data_fechamento',
        'status',
        'horas_validadas',
    ];

    protected $casts = [
        'id_encomenda' => 'integer',
        'horas_estimadas' => 'decimal:2',
        'valor_fechado' => 'decimal:2',
        'data_fechamento' => 'date',
        'status' => StatusFechamento::class,
        'horas_validadas' => 'decimal:2',
    ];
}
