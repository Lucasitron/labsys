<?php

namespace App\Modules\Producao\Models;

use Illuminate\Database\Eloquent\Model;

/**
 * Parâmetro 5S (producao.parametro_5s). CRUD trivial no Model (sem service):
 * leitura de flag/inteiro p/ advertência experimental e scheduler de mesas.
 */
class Parametro5S extends Model
{
    protected $table = 'producao.parametro_5s';

    protected $primaryKey = 'id_parametro';

    public $timestamps = false;

    protected $fillable = [
        'chave',
        'valor',
        'descricao',
    ];

    public static function valorDe(string $chave, ?string $padrao = null): ?string
    {
        return static::where('chave', $chave)->value('valor') ?? $padrao;
    }

    public static function inteiroDe(string $chave, int $padrao): int
    {
        $valor = static::valorDe($chave);

        if ($valor === null || ! is_numeric(trim($valor))) {
            return $padrao;
        }

        return (int) $valor;
    }

    public static function experimentalAtivo(): bool
    {
        return filter_var(static::valorDe('periodoExperimentalAtivo', 'false'), FILTER_VALIDATE_BOOLEAN);
    }
}
