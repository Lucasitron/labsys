<?php

namespace App\Services\Financeiro;

use App\Enums\Financeiro\StatusCompra;
use App\Shared\Auth\Caller;
use App\Models\Financeiro\SolicitacaoCompra;
use App\Policies\FinanceiroPolicy;
use App\Exceptions\ConflitoException;
use App\Exceptions\ResourceNotFoundException;

/**
 * Fluxo informativo de compras: registra com `REGISTRADA`, publica
 * `compra.solicitada.event` e conclui para `CONCLUIDA`. Sem bloqueio.
 * `marcarVisualizada` não existe (método Java sem rota — dead code).
 */
class CompraService
{
    public function __construct(private FinanceiroEventPublisher $publisher) {}

    public function registrar(array $dados, Caller $principal): SolicitacaoCompra
    {
        FinanceiroPolicy::exigeAdmin($principal);

        $compra = SolicitacaoCompra::create([
            'id_item_estoque' => $dados['idItemEstoque'],
            'quantidade' => number_format((float) $dados['quantidade'], 2, '.', ''),
            'valor_estimado' => isset($dados['valorEstimado'])
                ? number_format((float) $dados['valorEstimado'], 2, '.', '') : null,
            'status' => StatusCompra::REGISTRADA,
            'data_solicitacao' => today()->toDateString(),
        ]);

        $this->publisher->compraSolicitada((int) $compra->getKey());

        return $compra;
    }

    /** @return list<SolicitacaoCompra> */
    public function listar(?StatusCompra $status, Caller $principal): array
    {
        FinanceiroPolicy::exigeAdmin($principal);

        return SolicitacaoCompra::query()
            ->when($status !== null, fn ($q) => $q->where('status', $status))
            ->orderByDesc('id_solicitacao')
            ->get()->all();
    }

    public function detalhar(int $id, Caller $principal): SolicitacaoCompra
    {
        FinanceiroPolicy::exigeAdmin($principal);

        return SolicitacaoCompra::find($id)
            ?? throw new ResourceNotFoundException("Solicitação de compra não encontrada: {$id}");
    }

    public function concluir(int $id, Caller $principal): SolicitacaoCompra
    {
        FinanceiroPolicy::exigeAdmin($principal);

        $compra = $this->detalhar($id, $principal);

        if ($compra->status === StatusCompra::CONCLUIDA) {
            throw new ConflitoException('Solicitação de compra já concluída');
        }

        $compra->status = StatusCompra::CONCLUIDA;
        $compra->save();

        return $compra->refresh();
    }
}
