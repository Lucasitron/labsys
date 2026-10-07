<?php

namespace App\Modules\Notification\Support;

use App\Modules\Notification\Exceptions\LinkInvalidoException;

/**
 * Validação do campo `link` (reuse 1:1 do Java — D-6).
 *
 * Allowlist mínima: caminho interno (ex. `/notificacoes/12`) ou URL absoluta
 * `http(s)` com host. Qualquer outro formato (inclusive `javascript:` e
 * `data:`) é rejeitado com HTTP 422.
 */
final class LinkValidator
{
    private function __construct() {}

    public static function isValido(?string $link): bool
    {
        if ($link === null || trim($link) === '') {
            return true;
        }

        $valor = trim($link);

        if (str_starts_with($valor, '/')) {
            return ! str_contains($valor, ' ') && ! str_contains($valor, '\\');
        }

        $partes = parse_url($valor);
        if ($partes === false) {
            return false;
        }

        $scheme = strtolower((string) ($partes['scheme'] ?? ''));

        if ($scheme !== 'http' && $scheme !== 'https') {
            return false;
        }

        return isset($partes['host']) && trim((string) $partes['host']) !== '';
    }

    /** @throws LinkInvalidoException */
    public static function assertValido(?string $link): void
    {
        if (! self::isValido($link)) {
            throw new LinkInvalidoException(
                'Link da notificação inválido: use um caminho interno (/...) ou URL http(s)'
            );
        }
    }
}
