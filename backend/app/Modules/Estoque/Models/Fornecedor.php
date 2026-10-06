<?php

namespace App\Modules\Estoque\Models;

use Illuminate\Database\Eloquent\Model;

/** Fornecedor (estoque.fornecedor). */
class Fornecedor extends Model
{
    protected $table = 'estoque.fornecedor';

    protected $primaryKey = 'id_fornecedor';

    public $timestamps = false;

    protected $fillable = ['nome', 'contato', 'cnpj'];
}
