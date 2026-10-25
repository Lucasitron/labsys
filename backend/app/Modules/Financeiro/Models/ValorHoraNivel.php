<?php

namespace App\Modules\Financeiro\Models;

use Illuminate\Database\Eloquent\Model;

/** Valor/hora por nível de acesso (0-3), com vigência (append-only). */
class ValorHoraNivel extends Model
{
    protected $table = 'financeiro.valor_hora_nivel';

    protected $primaryKey = 'id_valor_hora';

    public $timestamps = false;

    protected $fillable = ['nivel_acesso', 'valor_hora', 'data_vigencia'];

    protected $casts = [
        'nivel_acesso' => 'integer',
        'valor_hora' => 'decimal:2',
        'data_vigencia' => 'date',
    ];
}
