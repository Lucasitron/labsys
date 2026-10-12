<?php

namespace App\Modules\Rh\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

class TreinamentoResource extends JsonResource
{
    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        return [
            'id' => (int) $this->id,
            'titulo' => $this->titulo,
            'descricao' => $this->descricao,
            'urlConteudo' => $this->url_conteudo,
            'idTutor' => (int) $this->id_tutor,
        ];
    }
}
