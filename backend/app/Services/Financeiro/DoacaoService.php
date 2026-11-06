<?php

namespace App\Services\Financeiro;

use App\Enums\Financeiro\TipoDoacao;
use App\Shared\Auth\Caller;
use App\Models\Financeiro\DoacaoRecurso;
use App\Policies\FinanceiroPolicy;

/** Registro e listagem de doações e recursos de projetos. */
class DoacaoService
{
    public function registrar(array $dados, Caller $principal): DoacaoRecurso
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
    public function listar(?TipoDoacao $tipo, ?string $inicio, ?string $fim, Caller $principal): array
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
