<?php

namespace App\Modules\Rh\Models;

use App\Modules\Rh\Enums\TipoCertificado;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/** Certificado emitido — `codigo_verificacao` UUID único, não sequencial. */
class CertificadoEmitido extends Model
{
    protected $table = 'rh.certificado_emitido';

    public $timestamps = false;

    protected $fillable = [
        'id_solicitacao',
        'id_funcionario',
        'tipo_certificado',
        'horas_certificadas',
        'data_emissao',
        'codigo_verificacao',
    ];

    protected $casts = [
        'tipo_certificado' => TipoCertificado::class,
        'horas_certificadas' => 'decimal:2',
        'data_emissao' => 'datetime',
    ];

    public function solicitacao(): BelongsTo
    {
        return $this->belongsTo(SolicitacaoCertificado::class, 'id_solicitacao');
    }

    public function funcionario(): BelongsTo
    {
        return $this->belongsTo(Funcionario::class, 'id_funcionario');
    }
}
