<?php

namespace App\Modules\Estoque\Models;

use Illuminate\Database\Eloquent\Model;

/** Localização física dos itens (estoque.localizacao). */
class Localizacao extends Model
{
    protected $table = 'estoque.localizacao';

    protected $primaryKey = 'id_localizacao';

    public $timestamps = false;

    protected $fillable = ['armario', 'prateleira', 'caixa', 'descricao'];
}
