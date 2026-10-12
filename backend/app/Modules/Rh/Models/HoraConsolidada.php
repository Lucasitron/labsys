<?php

namespace App\Modules\Rh\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/** Vínculo apontamento → certificado que o consolidou. */
class HoraConsolidada extends Model
{
    protected $table = 'rh.hora_consolidada';

    public $timestamps = false;

    protected $fillable = [
        'id_certificado',
        'id_apontamento',
        'horas',
        'data_consolidacao',
    ];

    protected $casts = [
        'horas' => 'decimal:2',
        'data_consolidacao' => 'datetime',
    ];

    public function certificado(): BelongsTo
    {
        return $this->belongsTo(CertificadoEmitido::class, 'id_certificado');
    }

    public function apontamento(): BelongsTo
    {
        return $this->belongsTo(ApontamentoHoras::class, 'id_apontamento');
    }
}
