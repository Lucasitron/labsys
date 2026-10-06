<?php

namespace App\Modules\Estoque\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/** Entrada de estoque / compra simples (estoque.entrada_estoque). */
class EntradaEstoque extends Model
{
    protected $table = 'estoque.entrada_estoque';

    protected $primaryKey = 'id_entrada';

    public $timestamps = false;

    protected $fillable = [
        'id_item',
        'id_fornecedor',
        'quantidade',
        'valor_unitario',
        'valor_total',
        'data_entrada',
        'nota_fiscal',
        'observacao',
        'responsavel',
    ];

    protected $casts = [
        'quantidade' => 'decimal:2',
        'valor_unitario' => 'decimal:2',
        'valor_total' => 'decimal:2',
        'data_entrada' => 'date',
    ];

    public function item(): BelongsTo
    {
        return $this->belongsTo(Item::class, 'id_item', 'id_item');
    }

    public function fornecedor(): BelongsTo
    {
        return $this->belongsTo(Fornecedor::class, 'id_fornecedor', 'id_fornecedor');
    }
}
