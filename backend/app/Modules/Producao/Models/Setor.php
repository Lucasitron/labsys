<?php

namespace App\Modules\Producao\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\HasMany;

/** Setor 5S (producao.setor). */
class Setor extends Model
{
    protected $table = 'producao.setor';

    protected $primaryKey = 'id_setor';

    public $timestamps = false;

    protected $fillable = [
        'numero',
        'nome',
        'descricao',
        'observacoes',
        'foto_correto_url',
        'foto_incorreto_url',
        'ativo',
    ];

    protected $casts = [
        'numero' => 'integer',
        'ativo' => 'boolean',
    ];

    public function materiais(): HasMany
    {
        return $this->hasMany(SetorMaterial::class, 'id_setor', 'id_setor');
    }

    public function sinalizacoes(): HasMany
    {
        return $this->hasMany(SetorSinalizacao::class, 'id_setor', 'id_setor');
    }

    public function checklists(): HasMany
    {
        return $this->hasMany(SetorChecklist::class, 'id_setor', 'id_setor');
    }

    public function responsaveis(): HasMany
    {
        return $this->hasMany(SetorResponsavel::class, 'id_setor', 'id_setor');
    }
}
