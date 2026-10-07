<?php

namespace Tests\Unit\Notification;

use App\Modules\Notification\Exceptions\LinkInvalidoException;
use App\Modules\Notification\Support\LinkValidator;
use PHPUnit\Framework\TestCase;

/** Allowlist de links (D-6, reuse 1:1 do Java). */
class LinkValidatorTest extends TestCase
{
    public function test_nulo_ou_branco_ok(): void
    {
        $this->assertTrue(LinkValidator::isValido(null));
        $this->assertTrue(LinkValidator::isValido(''));
        $this->assertTrue(LinkValidator::isValido('   '));
        LinkValidator::assertValido(null);
        $this->assertTrue(true);
    }

    public function test_caminho_interno_ok(): void
    {
        $this->assertTrue(LinkValidator::isValido('/notificacoes/12'));
        $this->assertFalse(LinkValidator::isValido('/com espaco'));
        $this->assertFalse(LinkValidator::isValido('/com\\barra'));
    }

    public function test_absoluta_http_ok_sem_host_nok(): void
    {
        $this->assertTrue(LinkValidator::isValido('https://fablab.local/notificacoes'));
        $this->assertTrue(LinkValidator::isValido('http://localhost:8080/api'));
        $this->assertFalse(LinkValidator::isValido('https://'));
        $this->assertFalse(LinkValidator::isValido('nao-url'));
    }

    public function test_esquemas_perigosos_rejeitados(): void
    {
        foreach (['javascript:alert(1)', 'data:text/html,oi', 'ftp://host/x', 'mailto:a@b.c'] as $link) {
            $this->assertFalse(LinkValidator::isValido($link), $link);

            try {
                LinkValidator::assertValido($link);
                $this->fail("{$link} deveria lançar");
            } catch (LinkInvalidoException) {
                $this->assertTrue(true);
            }
        }
    }
}
