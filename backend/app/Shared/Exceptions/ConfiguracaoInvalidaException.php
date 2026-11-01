<?php

namespace App\Shared\Exceptions;

class ConfiguracaoInvalidaException extends ApiException
{
    public function __construct(string $message = '')
    {
        parent::__construct($message, 422);
    }
}
