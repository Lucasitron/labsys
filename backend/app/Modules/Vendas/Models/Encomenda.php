<?php

namespace App\Modules\Vendas\Models;

use App\Modules\Vendas\Enums\StatusKanban;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasMany;

/** Encomenda do Kanban (vendas.encomenda). `versao` = lock otimista manual (409 em conflito). */
class Encomenda extends Model
{
    protected $table = 'vendas.encomenda';

    protected $primaryKey = 'id_encomenda';

    public $timestamps = false;

    protected $fillable = [
        'versao',
        'id_orcamento',
        'id_cliente',
        'data_criacao',
        'data_previsao_entrega',
        'status_kanban',
        'valor_final',
        'observacoes',
        'criado_por',
        'encomenda_origem_id',
    ];

    protected $casts = [
        'versao' => 'integer',
        'data_criacao' => 'date',
        'data_previsao_entrega' => 'date',
        'status_kanban' => StatusKanban::class,
        'valor_final' => 'decimal:2',
    ];

    public function cliente(): BelongsTo
    {
        return $this->belongsTo(Cliente::class, 'id_cliente', 'id_cliente');
    }

    public function historico(): HasMany
    {
        return $this->hasMany(HistoricoStatusEncomenda::class, 'id_encomenda', 'id_encomenda')
            ->orderBy('data_alteracao');
    }
}
