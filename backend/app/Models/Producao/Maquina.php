<?php

namespace App\Models\Producao;

use App\Enums\Producao\MaquinaStatus;
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
