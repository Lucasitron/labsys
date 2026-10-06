<?php

namespace App\Modules\Rh\Models;

use App\Modules\Rh\Enums\NivelAcesso;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/** Auditoria de evolução de nível (quem alterou + quando). */
class HistoricoNivel extends Model
{
    protected $table = 'rh.historico_nivel';

    public $timestamps = false;

    protected $fillable = [
        'id_funcionario',
        'nivel_antigo',
        'nivel_novo',
        'id_admin_alterou',
        'data_alteracao',
    ];

    protected $casts = [
        'nivel_antigo' => NivelAcesso::class,
        'nivel_novo' => NivelAcesso::class,
        'data_alteracao' => 'datetime',
    ];

    public function funcionario(): BelongsTo
    {
        return $this->belongsTo(Funcionario::class, 'id_funcionario');
    }

    public function admin(): BelongsTo
    {
        return $this->belongsTo(Funcionario::class, 'id_admin_alterou');
    }
}
