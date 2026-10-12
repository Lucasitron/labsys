<?php

namespace App\Modules\Estoque\Models;

use App\Modules\Estoque\Enums\Categoria;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/** Item de inventário (estoque.item). `versao` é coluna simples (lock pessimista cobre o MVP). */
class Item extends Model
{
    protected $table = 'estoque.item';

    protected $primaryKey = 'id_item';

    public $timestamps = false;

    protected $fillable = [
        'nome',
        'descricao',
        'categoria',
        'unidade_medida',
        'quantidade_atual',
        'estoque_minimo',
        'versao',
        'localizacao_id',
    ];

    protected $casts = [
        'categoria' => Categoria::class,
        'quantidade_atual' => 'decimal:2',
        'estoque_minimo' => 'decimal:2',
    ];

    public function localizacao(): BelongsTo
    {
        return $this->belongsTo(Localizacao::class, 'localizacao_id', 'id_localizacao');
    }
}
