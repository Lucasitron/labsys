<?php

namespace App\Modules\Vendas\Models;

use App\Modules\Vendas\Enums\TipoPessoa;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsToMany;
use Illuminate\Database\Eloquent\Relations\HasMany;

/** Cliente PF/PJ (vendas.cliente). Documento sai sempre mascarado (LGPD, na Resource). */
class Cliente extends Model
{
    protected $table = 'vendas.cliente';

    protected $primaryKey = 'id_cliente';

    public $timestamps = false;

    protected $fillable = [
        'tipo_pessoa',
        'nome_razao_social',
        'cpf_cnpj',
        'email',
        'telefone',
        'endereco',
        'data_cadastro',
        'criado_por',
    ];

    protected $casts = [
        'tipo_pessoa' => TipoPessoa::class,
        'data_cadastro' => 'date',
    ];

    public function tags(): BelongsToMany
    {
        return $this->belongsToMany(
            TagCliente::class,
            'vendas.cliente_tag',
            'id_cliente',
            'id_tag',
            'id_cliente',
            'id_tag',
        );
    }

    public function orcamentos(): HasMany
    {
        return $this->hasMany(Orcamento::class, 'id_cliente', 'id_cliente');
    }

    public function encomendas(): HasMany
    {
        return $this->hasMany(Encomenda::class, 'id_cliente', 'id_cliente');
    }
}
