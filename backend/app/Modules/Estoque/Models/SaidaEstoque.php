<?php

namespace App\Modules\Estoque\Models;

use App\Modules\Estoque\Enums\TipoSaida;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/** Saída de estoque (estoque.saida_estoque). */
class SaidaEstoque extends Model
{
    protected $table = 'estoque.saida_estoque';

    protected $primaryKey = 'id_saida';

    public $timestamps = false;

    protected $fillable = [
        'id_item',
        'quantidade',
        'tipo_saida',
        'id_referencia',
        'data_saida',
        'observacao',
        'responsavel',
    ];

    protected $casts = [
        'quantidade' => 'decimal:2',
        'tipo_saida' => TipoSaida::class,
        'data_saida' => 'datetime',
    ];

    public function item(): BelongsTo
    {
        return $this->belongsTo(Item::class, 'id_item', 'id_item');
    }
}
