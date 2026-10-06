<?php

namespace App\Modules\Rh\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/** Ponto diário consolidado do RFID — 1 registro por funcionário+data. */
class RegistroPontoDiario extends Model
{
    protected $table = 'rh.registro_ponto_diario';

    public $timestamps = false;

    protected $fillable = [
        'id_funcionario',
        'data',
        'hora_entrada',
        'hora_saida',
        'total_horas',
    ];

    protected $casts = [
        'data' => 'date',
        'hora_entrada' => 'datetime',
        'hora_saida' => 'datetime',
        'total_horas' => 'decimal:2',
    ];

    public function funcionario(): BelongsTo
    {
        return $this->belongsTo(Funcionario::class, 'id_funcionario');
    }
}
