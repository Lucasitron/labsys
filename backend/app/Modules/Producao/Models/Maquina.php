<?php

namespace App\Modules\Producao\Models;

use App\Modules\Producao\Enums\MaquinaStatus;
use Illuminate\Database\Eloquent\Model;

/** Máquina (producao.maquina). */
class Maquina extends Model
{
    protected $table = 'producao.maquina';

    protected $primaryKey = 'id_maquina';

    public $timestamps = false;

    protected $fillable = [
        'nome',
        'descricao',
        'status',
        'localizacao',
    ];

    protected $casts = [
        'status' => MaquinaStatus::class,
    ];

}
