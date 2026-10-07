<?php

namespace App\Modules\Estoque\Contracts;

use App\Modules\Estoque\Enums\StatusEmprestimo;
use App\Modules\Estoque\Models\Emprestimo;
use App\Modules\Estoque\Models\Item;
use App\Modules\Estoque\Services\ItemService;

class DefaultEstoqueContract implements EstoqueContract
{
    public function __construct(private ItemService $itens) {}

    public function itemExiste(int $idItem): bool
    {
        return Item::whereKey($idItem)->exists();
    }

    public function saldoDe(int $idItem): ?string
    {
        $saldo = Item::whereKey($idItem)->value('quantidade_atual');

        return $saldo === null ? null : number_format((float) $saldo, 2, '.', '');
    }

    public function baixarConsumo(array $itens, int $idReferencia): array
    {
        $saidas = $this->itens->baixarPorConsumo($itens, $idReferencia);

        return array_map(fn ($saida) => (int) $saida->getKey(), $saidas);
    }

    public function emprestimosAbertos(): int
    {
        return Emprestimo::whereIn('status', [StatusEmprestimo::ATIVO, StatusEmprestimo::ATRASADO])->count();
    }
}
