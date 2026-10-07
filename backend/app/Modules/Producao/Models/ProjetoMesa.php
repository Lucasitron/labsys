<?php

namespace App\Modules\Producao\Models;

use App\Modules\Producao\Enums\StatusProjetoMesa;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\HasMany;

/** Projeto individual de mesa (producao.projeto_mesa). QR = `fablab://projeto-mesa/{id}`. */
class ProjetoMesa extends Model
{
    protected $table = 'producao.projeto_mesa';

    protected $primaryKey = 'id_projeto_mesa';

    public $timestamps = false;

    protected $fillable = [
        'id_funcionario',
        'id_mesa',
        'nome_projeto',
        'tipo_projeto',
        'prazo_execucao',
        'data_inicio',
        'data_ultima_evolucao',
        'status',
        'qr_code_totem',
    ];

    protected $casts = [
        'prazo_execucao' => 'date',
        'data_inicio' => 'date',
        'data_ultima_evolucao' => 'date',
        'status' => StatusProjetoMesa::class,
    ];

    public function auditorias(): HasMany
    {
        return $this->hasMany(AuditoriaProjetoMesa::class, 'id_projeto_mesa', 'id_projeto_mesa');
    }
}
