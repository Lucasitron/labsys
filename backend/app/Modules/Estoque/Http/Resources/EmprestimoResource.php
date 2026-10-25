<?php

namespace App\Modules\Estoque\Http\Resources;

use App\Modules\Rh\Contracts\RhContract;
use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/** Empréstimo com `tomador` fail-soft (nome do RH ou `Pessoa #id`). */
class EmprestimoResource extends JsonResource
{
    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        return [
            'id' => (int) $this->id_emprestimo,
            'idItem' => (int) $this->id_item,
            'nomeItem' => $this->item?->nome,
            'idPessoa' => (int) $this->id_pessoa,
            'tomador' => $this->tomador(),
            'quantidade' => (string) $this->quantidade,
            'dataEmprestimo' => substr((string) $this->data_emprestimo, 0, 10),
            'dataDevolucaoPrevista' => substr((string) $this->data_devolucao_prevista, 0, 10),
            'dataDevolucaoReal' => $this->data_devolucao_real === null
                ? null : substr((string) $this->data_devolucao_real, 0, 10),
            'status' => $this->status->value,
            'observacao' => $this->observacao,
            'responsavel' => $this->responsavel,
        ];
    }

    private function tomador(): string
    {
        try {
            $nome = app(RhContract::class)->nomePessoa((int) $this->id_pessoa);
        } catch (\Throwable) {
            $nome = null;
        }

        return $nome ?? "Pessoa #{$this->id_pessoa}";
    }
}
