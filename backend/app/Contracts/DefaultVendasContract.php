<?php

namespace App\Contracts;

use App\Models\Vendas\Encomenda;
use App\Models\Vendas\Orcamento;

class DefaultVendasContract implements VendasContract
{
    public function dadosEncomenda(int $idEncomenda): ?array
    {
        $encomenda = Encomenda::find($idEncomenda);

        if ($encomenda === null) {
            return null;
        }

        return [
            'id' => (int) $encomenda->getKey(),
            'idCliente' => (int) $encomenda->id_cliente,
            'idOrcamento' => $encomenda->id_orcamento === null ? null : (int) $encomenda->id_orcamento,
            'valorFinal' => number_format((float) $encomenda->valor_final, 2, '.', ''),
            'statusKanban' => $encomenda->status_kanban->value,
            'dataCriacao' => (string) $encomenda->data_criacao->toDateString(),
            'dataPrevisao' => $encomenda->data_previsao_entrega === null
                ? null : (string) $encomenda->data_previsao_entrega->toDateString(),
        ];
    }

    public function dadosOrcamento(int $idOrcamento): ?array
    {
        $orcamento = Orcamento::find($idOrcamento);

        if ($orcamento === null) {
            return null;
        }

        return [
            'id' => (int) $orcamento->getKey(),
            'idCliente' => (int) $orcamento->id_cliente,
            'valorTotal' => number_format((float) $orcamento->valor_total, 2, '.', ''),
            'status' => $orcamento->status->value,
        ];
    }
}
