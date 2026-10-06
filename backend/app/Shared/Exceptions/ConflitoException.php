<?php

namespace App\Shared\Exceptions;

/** Conflito de estado/versão (lock otimista, duplicidade semântica) → 409. */
class ConflitoException extends \RuntimeException
{
    public function __construct(string $message = 'Conflito de estado')
    {
        parent::__construct($message);
    }
}
