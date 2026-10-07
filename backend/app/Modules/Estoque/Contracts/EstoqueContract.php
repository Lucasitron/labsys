<?php

namespace App\Modules\Estoque\Contracts;

/**
 * Fronteira pública do Estoque p/ Vendas (M4: itemExiste/saldoDe) e Produção
 * (M6: baixarConsumo idempotente) — in-process, sem HTTP. Só o consumido.
 */
interface EstoqueContract
{
    public function itemExiste(int $idItem): bool;

    /** Saldo atual do item (decimal `string`) ou null quando inexistente. */
    public function saldoDe(int $idItem): ?string;

    /**
     * Baixa idempotente por referência (BOM/produção, retry seguro).
     *
     * @param  list<array{idItem:int,quantidadeConsumida:string|float|int}>  $itens
     * @return list<int> ids das saídas CONSUMO criadas ([] se idempotente/vazio)
     */
    public function baixarConsumo(array $itens, int $idReferencia): array;

    /**
     * Empréstimos em aberto = ATIVO+ATRASADO (≡ `?status=ativos` E-7;
     * M8/Dashboard só invoca se Admin).
     */
    public function emprestimosAbertos(): int;
}
