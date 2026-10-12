<?php

namespace App\Modules\Producao\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/** Registro de uso de máquina (producao.historico_uso_maquina). */
class HistoricoUsoMaquina extends Model
{
    protected $table = 'producao.historico_uso_maquina';

    protected $primaryKey = 'id_uso';

    public $timestamps = false;

    protected $fillable = [
        'id_maquina',
        'id_funcionario',
        'data_inicio',
        'data_fim',
        'horas_uso',
        'observacao',
    ];

    protected $casts = [
        'data_inicio' => 'datetime',
        'data_fim' => 'datetime',
        'horas_uso' => 'decimal:2',
    ];

    public function maquina(): BelongsTo
    {
        return $this->belongsTo(Maquina::class, 'id_maquina', 'id_maquina');
    }
}
