<?php

namespace App\Modules\Notification\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/** Notificação no contrato do front (keys EN 1:1 — D-1; só os paths viram PT). */
class NotificationResource extends JsonResource
{
    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        return [
            'id' => (int) $this->getKey(),
            'type' => $this->tipo,
            'title' => $this->titulo,
            'body' => $this->mensagem,
            'read' => (bool) $this->lida,
            'createdAt' => $this->criada_em instanceof \DateTimeInterface
                ? $this->criada_em->toIso8601String()
                : (string) $this->criada_em,
            'link' => $this->link,
            'channel' => $this->canal,
        ];
    }
}
