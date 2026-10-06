<?php

namespace App\Modules\Rh\Models;

use App\Modules\Rh\Enums\StatusApontamento;
use App\Modules\Rh\Enums\TipoApontamento;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/** Horas em ENCOMENDA/PROJETO. Nasce PENDENTE; consolidado=true após certificado. */
class ApontamentoHoras extends Model
{
    protected $table = 'rh.apontamento_horas';

    public $timestamps = false;

    protected $fillable = [
        'id_funcionario',
        'tipo',
        'id_referencia',
        'data',
        'horas_trabalhadas',
        'hora_inicio',
        'hora_fim',
        'motivo_rejeicao',
        'descricao_atividade',
        'status',
        'id_admin_validador',
        'data_validacao',
        'consolidado',
    ];

    protected $casts = [
        'tipo' => TipoApontamento::class,
        'data' => 'date',
        'horas_trabalhadas' => 'decimal:2',
        'status' => StatusApontamento::class,
        'data_validacao' => 'datetime',
        'consolidado' => 'boolean',
    ];

    public function funcionario(): BelongsTo
    {
        return $this->belongsTo(Funcionario::class, 'id_funcionario');
    }
}
