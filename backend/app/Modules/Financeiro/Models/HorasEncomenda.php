<?php

namespace App\Modules\Financeiro\Models;

use Illuminate\Database\Eloquent\Model;

/** Horas validadas do RH por (encomenda, funcionário, data) — upsert. */
class HorasEncomenda extends Model
{
    protected $table = 'financeiro.horas_encomenda';

    protected $primaryKey = 'id_horas';

    public $timestamps = false;

    protected $fillable = [
        'id_encomenda',
        'id_funcionario',
        'nivel_acesso',
        'horas',
        'data_registro',
    ];

    protected $casts = [
        'id_encomenda' => 'integer',
        'id_funcionario' => 'integer',
        'nivel_acesso' => 'integer',
        'horas' => 'decimal:2',
        'data_registro' => 'date',
    ];
}
