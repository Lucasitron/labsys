<?php

namespace App\Modules\Producao\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/** Projeto de mesa (QR = `fablab://projeto-mesa/{id}`). */
class ProjetoMesaResource extends JsonResource
{
    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        return [
            'id' => (int) $this->id_projeto_mesa,
            'idFuncionario' => (int) $this->id_funcionario,
            'idMesa' => (int) $this->id_mesa,
            'nomeProjeto' => $this->nome_projeto,
            'tipoProjeto' => $this->tipo_projeto,
            'prazoExecucao' => $this->prazo_execucao === null
                ? null : substr((string) $this->prazo_execucao, 0, 10),
            'dataInicio' => substr((string) $this->data_inicio, 0, 10),
            'dataUltimaEvolucao' => $this->data_ultima_evolucao === null
                ? null : substr((string) $this->data_ultima_evolucao, 0, 10),
            'status' => $this->status->value,
            'qrCodeTotem' => $this->qr_code_totem,
        ];
    }
}
