<?php

namespace App\Modules\Estoque\Services;

use App\Modules\Estoque\EstoquePrincipal;
use App\Modules\Estoque\Events\CompraSolicitadaEvent;
use App\Modules\Estoque\Models\EntradaEstoque;
use App\Modules\Estoque\Models\Fornecedor;
use App\Modules\Estoque\Models\Item;
use App\Modules\Estoque\Policies\EstoquePolicy;
use App\Shared\Exceptions\ResourceNotFoundException;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Log;

/** Entradas / compras simples (regras só aqui). */
class EntradaService
{
    public function __construct(private ItemService $itens) {}

    public function registrar(array $dados, EstoquePrincipal $principal): EntradaEstoque
    {
        EstoquePolicy::exigeEdicaoMovimentacao($principal);

        return DB::transaction(function () use ($dados, $principal) {
            $item = Item::whereKey((int) $dados['idItem'])->lockForUpdate()->first()
                ?? throw new ResourceNotFoundException("Item não encontrado: {$dados['idItem']}");

            $fornecedor = Fornecedor::find((int) $dados['idFornecedor'])
                ?? throw new ResourceNotFoundException("Fornecedor não encontrado: {$dados['idFornecedor']}");

            $quantidade = number_format((float) $dados['quantidade'], 2, '.', '');
            $unitario = number_format((float) $dados['valorUnitario'], 2, '.', '');
            // qtd × unit em centavos (exato, sem bcmath): total = (c1 × c2) / 10000.
            $total = number_format(
                ((int) round((float) $quantidade * 100) * (int) round((float) $unitario * 100)) / 10000,
                2, '.', '',
            );

            $entrada = EntradaEstoque::create([
                'id_item' => $item->getKey(),
                'id_fornecedor' => $fornecedor->getKey(),
                'quantidade' => $quantidade,
                'valor_unitario' => $unitario,
                'valor_total' => $total,
                'data_entrada' => $dados['dataEntrada'] ?? today()->toDateString(),
                'nota_fiscal' => $dados['notaFiscal'] ?? null,
                'observacao' => $dados['observacao'] ?? null,
                'responsavel' => $dados['responsavel'] ?? null,
            ]);

            $item->quantidade_atual = number_format(
                ((int) round((float) $item->quantidade_atual * 100) + (int) round((float) $quantidade * 100)) / 100,
                2, '.', '',
            );
            $item->save();

            // Compra simples = pedido de compra do fluxo (fail-soft, nunca 500).
            try {
                event(new CompraSolicitadaEvent(
                    (int) $entrada->getKey(),
                    (int) $item->getKey(),
                    (int) $fornecedor->getKey(),
                    $quantidade,
                    $unitario,
                    (string) $entrada->valor_total,
                    substr((string) $entrada->data_entrada, 0, 10),
                    $entrada->nota_fiscal,
                ));
            } catch (\Throwable $e) {
                Log::warning("Falha ao publicar compra.solicitada.event da entrada {$entrada->getKey()}: {$e->getMessage()}");
            }

            $this->itens->verificarEstoqueBaixo($item);

            return $entrada->refresh()->load(['item', 'fornecedor']);
        });
    }

    /** @return list<EntradaEstoque> */
    public function listar(?int $idItem, EstoquePrincipal $principal): array
    {
        EstoquePolicy::exigeLeitura($principal);

        return EntradaEstoque::query()
            ->with(['item', 'fornecedor'])
            ->when($idItem !== null, fn ($q) => $q->where('id_item', $idItem))
            ->orderBy('id_entrada')
            ->get()
            ->all();
    }

    public function buscar(int $id, EstoquePrincipal $principal): EntradaEstoque
    {
        EstoquePolicy::exigeLeitura($principal);

        return EntradaEstoque::with(['item', 'fornecedor'])->find($id)
            ?? throw new ResourceNotFoundException("Entrada não encontrada: {$id}");
    }
}
