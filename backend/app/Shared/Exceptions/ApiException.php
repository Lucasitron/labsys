<?php

namespace App\Shared\Exceptions;

class ApiException extends \RuntimeException
{
    public function __construct(string $message, public readonly int $status)
    {
        parent::__construct($message);
    }
}