<?php

namespace App\Modules\Vendas\Models;

use Illuminate\Database\Eloquent\Model;

/** Tag de segmentação de marketing (vendas.tag_cliente). CRUD trivial, sem service. */
class TagCliente extends Model
{
    protected $table = 'vendas.tag_cliente';

    protected $primaryKey = 'id_tag';

    public $timestamps = false;

    protected $fillable = [
        'nome',
        'cor',
    ];
}
