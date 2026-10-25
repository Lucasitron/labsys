<?php

namespace App\Modules\Financeiro\Models;

use App\Modules\Financeiro\Enums\TipoCategoria;
use Illuminate\Database\Eloquent\Model;

/** Categoria financeira (financeiro.categoria_financeira). */
class CategoriaFinanceira extends Model
{
    protected $table = 'financeiro.categoria_financeira';

    protected $primaryKey = 'id_categoria';

    public $timestamps = false;

    protected $fillable = ['nome', 'tipo', 'descricao'];

    protected $casts = ['tipo' => TipoCategoria::class];
}
