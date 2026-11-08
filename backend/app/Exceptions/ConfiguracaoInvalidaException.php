<?php

namespace App\Exceptions;

class ConfiguracaoInvalidaException extends ApiException
{
    public function __construct(string $message = '')
    {
        parent::__construct($message, 422);
    }
}
