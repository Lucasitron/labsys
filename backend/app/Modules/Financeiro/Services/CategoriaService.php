<?php

namespace App\Modules\Financeiro\Services;

use App\Modules\Financeiro\Enums\TipoCategoria;
use App\Modules\Financeiro\FinanceiroPrincipal;
use App\Modules\Financeiro\Models\CategoriaFinanceira;
use App\Modules\Financeiro\Policies\FinanceiroPolicy;

/** Cadastro e listagem de categorias financeiras (receita/despesa). */
class CategoriaService
{
    /** @return list<CategoriaFinanceira> */
    public function listar(?TipoCategoria $tipo, FinanceiroPrincipal $principal): array
    {
        FinanceiroPolicy::exigeAdmin($principal);

        return CategoriaFinanceira::query()
            ->when($tipo !== null, fn ($q) => $q->where('tipo', $tipo))
            ->orderBy('id_categoria')
            ->get()->all();
    }

    public function criar(array $dados, FinanceiroPrincipal $principal): CategoriaFinanceira
    {
        FinanceiroPolicy::exigeAdmin($principal);

        return CategoriaFinanceira::create([
            'nome' => trim((string) $dados['nome']),
            'tipo' => TipoCategoria::from($dados['tipo']),
            'descricao' => $dados['descricao'] ?? null,
        ]);
    }
}
