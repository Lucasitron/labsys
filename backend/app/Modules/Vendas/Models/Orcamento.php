<?php

namespace App\Modules\Vendas\Models;

use App\Modules\Vendas\Enums\StatusOrcamento;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasMany;

/** Orçamento com itens (vendas.orcamento). Total calculado no service, nunca do payload. */
class Orcamento extends Model
{
    protected $table = 'vendas.orcamento';

    protected $primaryKey = 'id_orcamento';

    public $timestamps = false;

    protected $fillable = [
        'id_cliente',
        'data_criacao',
        'validade',
        'valor_total',
        'status',
        'observacoes',
        'criado_por',
    ];

    protected $casts = [
        'data_criacao' => 'date',
        'validade' => 'date',
        'valor_total' => 'decimal:2',
        'status' => StatusOrcamento::class,
    ];

    public function itens(): HasMany
    {
        return $this->hasMany(ItemOrcamento::class, 'id_orcamento', 'id_orcamento');
    }

    public function cliente(): BelongsTo
    {
        return $this->belongsTo(Cliente::class, 'id_cliente', 'id_cliente');
    }
}
