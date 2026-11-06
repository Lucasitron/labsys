<?php

namespace App\Models\Producao;

use App\Enums\Producao\StatusProjetoMesa;
use Illuminate\Database\Eloquent\Model;

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

}
