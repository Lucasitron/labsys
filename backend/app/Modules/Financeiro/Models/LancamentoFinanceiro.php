<?php

namespace App\Modules\Financeiro\Models;

use App\Modules\Financeiro\Enums\StatusLancamento;
use App\Modules\Financeiro\Enums\TipoLancamento;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/** Lançamento financeiro — conta a pagar (SAIDA) ou a receber (ENTRADA). */
class LancamentoFinanceiro extends Model
{
    protected $table = 'financeiro.lancamento_financeiro';

    protected $primaryKey = 'id_lancamento';

    public $timestamps = false;

    protected $fillable = [
        'id_categoria',
        'tipo',
        'valor',
        'data_vencimento',
        'data_pagamento',
        'status',
        'id_referencia_externa',
        'observacao',
    ];

    protected $casts = [
        'tipo' => TipoLancamento::class,
        'valor' => 'decimal:2',
        'data_vencimento' => 'date',
        'data_pagamento' => 'date',
        'status' => StatusLancamento::class,
    ];

    public function categoria(): BelongsTo
    {
        return $this->belongsTo(CategoriaFinanceira::class, 'id_categoria', 'id_categoria');
    }
}
