<?php

namespace App\Modules\Financeiro\Services;

use App\Modules\Financeiro\Enums\TipoDoacao;
use App\Modules\Financeiro\FinanceiroPrincipal;
use App\Modules\Financeiro\Models\DoacaoRecurso;
use App\Modules\Financeiro\Policies\FinanceiroPolicy;

/** Registro e listagem de doações e recursos de projetos. */
class DoacaoService
{
    public function registrar(array $dados, FinanceiroPrincipal $principal): DoacaoRecurso
    {
        FinanceiroPolicy::exigeAdmin($principal);

        return DoacaoRecurso::create([
            'tipo' => TipoDoacao::from($dados['tipo']),
            'origem' => trim((string) $dados['origem']),
            'valor' => number_format((float) $dados['valor'], 2, '.', ''),
            'data_recebimento' => $dados['dataRecebimento'],
            'id_projeto_associado' => $dados['idProjetoAssociado'] ?? null,
        ]);
    }

    /** @return list<DoacaoRecurso> */
    public function listar(?TipoDoacao $tipo, ?string $inicio, ?string $fim, FinanceiroPrincipal $principal): array
    {
        FinanceiroPolicy::exigeAdmin($principal);

        return DoacaoRecurso::query()
            ->when($tipo !== null, fn ($q) => $q->where('tipo', $tipo))
            ->when($inicio !== null && $fim !== null,
                fn ($q) => $q->whereBetween('data_recebimento', [$inicio, $fim]))
            ->orderByDesc('id_doacao')
            ->get()->all();
    }
}
