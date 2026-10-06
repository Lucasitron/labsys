<?php

namespace App\Modules\Vendas\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/** Encomenda (lista) + detalhe com itens do orçamento e histórico. */
class EncomendaResource extends JsonResource
{
    /** @var list<mixed> */
    public array $itensOrcamento = [];

    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        $base = [
            'id' => (int) $this->id_encomenda,
            'idOrcamento' => $this->id_orcamento === null ? null : (int) $this->id_orcamento,
            'idCliente' => (int) $this->id_cliente,
            'clienteNome' => $this->cliente?->nome_razao_social,
            'dataCriacao' => substr((string) $this->data_criacao, 0, 10),
            'dataPrevisaoEntrega' => $this->data_previsao_entrega === null
                ? null : substr((string) $this->data_previsao_entrega, 0, 10),
            'statusKanban' => $this->status_kanban->value,
            'valorFinal' => (string) $this->valor_final,
            'versao' => (int) $this->versao,
            'criadoPor' => (int) $this->criado_por,
            'encomendaOrigemId' => $this->encomenda_origem_id === null
                ? null : (int) $this->encomenda_origem_id,
        ];

        if ($this->relationLoaded('historico')) {
            $base['observacoes'] = $this->observacoes;
            $base['itens'] = array_map(
                fn ($i) => (new ItemOrcamentoResource($i))->toArray($request),
                $this->itensOrcamento,
            );
            $base['historico'] = HistoricoResource::collection($this->historico)->toArray($request);
        }

        return $base;
    }
}
