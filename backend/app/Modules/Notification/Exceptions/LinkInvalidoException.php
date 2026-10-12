<?php

namespace App\Modules\Notification\Exceptions;

/** Link de notificação fora do formato/allowlist (D-6 → HTTP 422). */
class LinkInvalidoException extends \RuntimeException
{
    public function __construct(string $message = 'Link da notificação inválido')
    {
        parent::__construct($message);
    }
}
