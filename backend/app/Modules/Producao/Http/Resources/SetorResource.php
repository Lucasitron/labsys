<?php

namespace App\Modules\Producao\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/** Setor 5S detalhado (materiais/sinalizações/checklist/responsáveis). */
class SetorResource extends JsonResource
{
    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        return [
            'id' => (int) $this->id_setor,
            'numero' => (int) $this->numero,
            'nome' => $this->nome,
            'descricao' => $this->descricao,
            'observacoes' => $this->observacoes,
            'fotoCorretoUrl' => $this->foto_correto_url,
            'fotoIncorretoUrl' => $this->foto_incorreto_url,
            'ativo' => (bool) $this->ativo,
            'materiais' => MaterialResource::collection($this->whenLoaded('materiais'))->toArray($request),
            'sinalizacoes' => SinalizacaoResource::collection($this->whenLoaded('sinalizacoes'))->toArray($request),
            'checklists' => ChecklistResource::collection($this->whenLoaded('checklists'))->toArray($request),
            'responsaveis' => ResponsavelResource::collection($this->whenLoaded('responsaveis'))->toArray($request),
        ];
    }
}
