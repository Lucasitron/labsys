<?php

namespace App\Modules\Producao\Models;

use App\Modules\Producao\Enums\MaquinaStatus;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\HasMany;

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

    public function usos(): HasMany
    {
        return $this->hasMany(HistoricoUsoMaquina::class, 'id_maquina', 'id_maquina')
            ->orderByDesc('data_inicio');
    }
}
