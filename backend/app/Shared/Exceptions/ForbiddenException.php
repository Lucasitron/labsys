<?php

namespace App\Shared\Exceptions;

class ForbiddenException extends \RuntimeException
{
    public function __construct(string $message = 'Acesso negado')
    {
        parent::__construct($message);
    }
}
