<?php

namespace App\Shared\Exceptions;

class ForbiddenException extends ApiException
{
    public function __construct(string $message = 'Acesso negado')
    {
        parent::__construct($message, 403);
    }
}
