<?php

namespace App\Modules\Vendas\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/** Venda manual externa (vendas.registro_marketplace). Líquido = bruto − taxa (no service). */
class RegistroMarketplace extends Model
{
    protected $table = 'vendas.registro_marketplace';

    protected $primaryKey = 'id_registro';

    public $timestamps = false;

    protected $fillable = [
        'id_encomenda',
        'plataforma',
        'codigo_externo',
        'data_venda',
        'valor_taxa',
    ];

    protected $casts = [
        'data_venda' => 'date',
        'valor_taxa' => 'decimal:2',
    ];

    public function encomenda(): BelongsTo
    {
        return $this->belongsTo(Encomenda::class, 'id_encomenda', 'id_encomenda');
    }
}
