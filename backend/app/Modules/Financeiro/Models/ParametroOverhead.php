<?php

namespace App\Modules\Financeiro\Models;

use Illuminate\Database\Eloquent\Model;

/** Taxa de overhead por hora, com vigência (append-only). */
class ParametroOverhead extends Model
{
    protected $table = 'financeiro.parametro_overhead';

    protected $primaryKey = 'id_parametro';

    public $timestamps = false;

    protected $fillable = ['valor_taxa_hora', 'data_vigencia'];

    protected $casts = [
        'valor_taxa_hora' => 'decimal:4',
        'data_vigencia' => 'date',
    ];
}
