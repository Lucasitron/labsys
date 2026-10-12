<?php

namespace App\Modules\Producao\Models;

use App\Modules\Producao\Enums\StatusProjetoMesa;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/** Auditoria de projeto de mesa (producao.auditoria_projeto_mesa). Espelha o status. */
class AuditoriaProjetoMesa extends Model
{
    protected $table = 'producao.auditoria_projeto_mesa';

    protected $primaryKey = 'id_auditoria';

    public $timestamps = false;

    protected $fillable = [
        'id_projeto_mesa',
        'data_auditoria',
        'resultado',
        'acao_tomada',
        'id_admin_responsavel',
    ];

    protected $casts = [
        'data_auditoria' => 'date',
        'resultado' => StatusProjetoMesa::class,
    ];

    public function projetoMesa(): BelongsTo
    {
        return $this->belongsTo(ProjetoMesa::class, 'id_projeto_mesa', 'id_projeto_mesa');
    }
}
