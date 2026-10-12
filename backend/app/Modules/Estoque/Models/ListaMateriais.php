<?php

namespace App\Modules\Estoque\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\HasMany;

/** Lista de Materiais — BOM (estoque.lista_materiais). */
class ListaMateriais extends Model
{
    protected $table = 'estoque.lista_materiais';

    protected $primaryKey = 'id_bom';

    public $timestamps = false;

    protected $fillable = ['id_produto_servico', 'nome', 'versao', 'editavel'];

    protected $casts = [
        'editavel' => 'boolean',
    ];

    public function itens(): HasMany
    {
        return $this->hasMany(ItemBom::class, 'id_bom', 'id_bom');
    }
}
