<?php

namespace Tests\Unit\Auth;

use App\Modules\Auth\Models\TokenIntegracao;
use App\Modules\Auth\Services\TokenIntegracaoService;
use App\Shared\Exceptions\ConfiguracaoInvalidaException;
use App\Shared\Exceptions\ResourceNotFoundException;
use Tests\TestCase;

class TokenIntegracaoServiceTest extends TestCase
{
    public function test_criar_exibe_chave_uma_vez_e_persiste_so_hash(): void
    {
        $criado = app(TokenIntegracaoService::class)->criar('ESP32 Porta');

        $this->assertNotEmpty($criado['chave']);
        $this->assertSame(substr($criado['chave'], 0, 12), $criado['prefixo']);

        $row = TokenIntegracao::find($criado['id']);
        $this->assertSame(hash('sha256', $criado['chave']), $row->hash);
        $this->assertStringNotContainsString($criado['chave'], (string) json_encode($row->toArray()));
    }

    public function test_criar_nome_duplicado_ou_vazio_lanca_422(): void
    {
        $svc = app(TokenIntegracaoService::class);
        $svc->criar('Integracao A');

        $this->expectException(ConfiguracaoInvalidaException::class);
        $svc->criar('Integracao A');
    }

    public function test_revogar_e_soft_e_segunda_revogacao_da_404(): void
    {
        $svc = app(TokenIntegracaoService::class);
        $criado = $svc->criar('Integracao B');

        $svc->revogar($criado['id']);
        $this->assertTrue(TokenIntegracao::find($criado['id'])->revogado);
        $this->assertSame([], $svc->listarAtivos());

        $this->expectException(ResourceNotFoundException::class);
        $svc->revogar($criado['id']);
    }

    public function test_revogar_inexistente_da_404(): void
    {
        $this->expectException(ResourceNotFoundException::class);
        app(TokenIntegracaoService::class)->revogar(999999);
    }
}
