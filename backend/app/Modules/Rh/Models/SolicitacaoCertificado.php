<?php

namespace App\Modules\Rh\Models;

use App\Modules\Rh\Enums\StatusSolicitacao;
use App\Modules\Rh\Enums\TipoCertificado;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/** Solicitação de certificado (decidida por Admin; trava pessimista na decisão). */
class SolicitacaoCertificado extends Model
{
    protected $table = 'rh.solicitacao_certificado';

    public $timestamps = false;

    protected $fillable = [
        'id_funcionario',
        'tipo_certificado',
        'data_solicitacao',
        'horas_solicitadas',
        'status',
        'id_admin_aprovador',
        'data_decisao',
        'observacao',
    ];

    protected $casts = [
        'tipo_certificado' => TipoCertificado::class,
        'data_solicitacao' => 'datetime',
        'horas_solicitadas' => 'decimal:2',
        'status' => StatusSolicitacao::class,
        'data_decisao' => 'datetime',
    ];

    public function funcionario(): BelongsTo
    {
        return $this->belongsTo(Funcionario::class, 'id_funcionario');
    }
}
