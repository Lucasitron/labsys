<?php

namespace App\Modules\Auth\Models;

use Illuminate\Database\Eloquent\Model;

/** Parâmetro global do sistema (auth.parametro_sistema, PK chave). */
class ParametroSistema extends Model
{
    protected $table = 'auth.parametro_sistema';

    public $timestamps = false;

    public $incrementing = false;

    protected $primaryKey = 'chave';

    protected $keyType = 'string';

    protected $fillable = ['chave', 'valor'];
}
