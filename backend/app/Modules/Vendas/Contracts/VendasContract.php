<?php

namespace App\Modules\Vendas\Contracts;

/**
 * Fronteira pública do Vendas p/ Produção (M6: encomenda) e Financeiro
 * (M5: valores) — in-process, sem HTTP. Só o consumido (2 métodos).
 */
interface VendasContract
{
    /**
     * Dados da encomenda (id/idCliente/idOrcamento/valorFinal/statusKanban/
     * dataCriacao/dataPrevisao). Null quando inexistente.
     */
    public function dadosEncomenda(int $idEncomenda): ?array;

    /**
     * Dados do orçamento (id/idCliente/valorTotal/status). Null quando
     * inexistente.
     */
    public function dadosOrcamento(int $idOrcamento): ?array;
}
