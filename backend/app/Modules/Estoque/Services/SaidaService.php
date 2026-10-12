<?php

namespace App\Modules\Estoque\Services;

use App\Modules\Estoque\Enums\TipoSaida;
use App\Modules\Estoque\EstoquePrincipal;
use App\Modules\Estoque\Models\Item;
use App\Modules\Estoque\Models\SaidaEstoque;
use App\Modules\Estoque\Policies\EstoquePolicy;
use App\Shared\Exceptions\ResourceNotFoundException;
use App\Shared\Exceptions\SaldoInsuficienteException;
use Illuminate\Support\Facades\DB;

/** Saídas manuais — consumo, perda, ajuste (regras só aqui). */
class SaidaService
{
    public function __construct(private ItemService $itens) {}

    public function registrar(array $dados, EstoquePrincipal $principal): SaidaEstoque
    {
        EstoquePolicy::exigeEdicaoMovimentacao($principal);

        return DB::transaction(function () use ($dados) {
            $item = Item::whereKey((int) $dados['idItem'])->lockForUpdate()->first()
                ?? throw new ResourceNotFoundException("Item não encontrado: {$dados['idItem']}");

            $quantidade = number_format((float) $dados['quantidade'], 2, '.', '');
            $novoSaldo = (int) round((float) $item->quantidade_atual * 100)
                - (int) round((float) $quantidade * 100);

            if ($novoSaldo < 0) {
                throw new SaldoInsuficienteException(
                    "Estoque insuficiente para o item {$item->nome}: saldo atual {$item->quantidade_atual}, solicitado {$quantidade}",
                );
            }

            $item->quantidade_atual = number_format($novoSaldo / 100, 2, '.', '');
            $item->save();

            $saida = SaidaEstoque::create([
                'id_item' => $item->getKey(),
                'quantidade' => $quantidade,
                'tipo_saida' => TipoSaida::from($dados['tipoSaida']),
                'id_referencia' => $dados['idReferencia'] ?? null,
                'data_saida' => now(),
                'observacao' => $dados['observacao'] ?? null,
                'responsavel' => $dados['responsavel'] ?? null,
            ]);

            $this->itens->verificarEstoqueBaixo($item);

            return $saida;
        });
    }

    /** @return list<SaidaEstoque> */
    public function listar(?int $idItem, EstoquePrincipal $principal): array
    {
        EstoquePolicy::exigeLeitura($principal);

        return SaidaEstoque::query()
            ->with('item')
            ->when($idItem !== null, fn ($q) => $q->where('id_item', $idItem))
            ->orderBy('id_saida')
            ->get()
            ->all();
    }

    public function buscar(int $id, EstoquePrincipal $principal): SaidaEstoque
    {
        EstoquePolicy::exigeLeitura($principal);

        return SaidaEstoque::with('item')->find($id)
            ?? throw new ResourceNotFoundException("Saída não encontrada: {$id}");
    }
}
