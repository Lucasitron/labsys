<?php

namespace Tests\Feature\Auth;

use App\Modules\Auth\Enums\Role;
use Tests\TestCase;

class ConfiguracoesTest extends TestCase
{
    public function test_sistema_obter_e_atualizar(): void
    {
        $admin = $this->criarAdmin();
        $headers = $this->authHeader($admin);

        $this->getJson('/api/configuracoes/sistema', $headers)
            ->assertOk()
            ->assertJson(['identidade' => ['nomeFablab' => 'FabLab IFPR — Curitiba']])
            ->assertJsonStructure(['identidade', 'cadenciaChecklist5S', 'cadenciaAuditoria5S', 'tokens']);

        $this->putJson('/api/configuracoes/sistema', [
            'identidade' => ['nomeFablab' => 'Lab Novo', 'logo' => ''],
            'cadenciaChecklist5S' => 'Quinzenal',
            'cadenciaAuditoria5S' => 'Mensal',
        ], $headers)
            ->assertOk()
            ->assertJson(['cadenciaChecklist5S' => 'Quinzenal']);
    }

    public function test_sistema_cadencia_invalida_da_422(): void
    {
        $admin = $this->criarAdmin();
        $headers = $this->authHeader($admin);

        $this->putJson('/api/configuracoes/sistema', [
            'identidade' => ['nomeFablab' => 'Lab'],
            'cadenciaChecklist5S' => 'Diaria',
        ], $headers)->assertStatus(422);

        $this->putJson('/api/configuracoes/sistema', [
            'cadenciaChecklist5S' => 'Semanal',
        ], $headers)->assertStatus(422);
    }

    public function test_tokens_ciclo_completo(): void
    {
        $admin = $this->criarAdmin();
        $headers = $this->authHeader($admin);

        $this->getJson('/api/configuracoes/tokens', $headers)->assertOk()->assertJson([]);

        $criado = $this->postJson('/api/configuracoes/tokens', ['nome' => 'ESP32'], $headers)
            ->assertCreated()
            ->assertJsonStructure(['id', 'nome', 'prefixo', 'criadoEm', 'chave'])
            ->json();

        $this->assertNotEmpty($criado['chave']);

        // lista não expõe chave nem hash
        $lista = $this->getJson('/api/configuracoes/tokens', $headers)->assertOk()->json();
        $this->assertCount(1, $lista);
        $this->assertArrayNotHasKey('chave', $lista[0]);
        $this->assertArrayNotHasKey('hash', $lista[0]);

        $this->postJson('/api/configuracoes/tokens', ['nome' => 'ESP32'], $headers)
            ->assertStatus(422);

        $this->deleteJson("/api/configuracoes/tokens/{$criado['id']}", [], $headers)
            ->assertNoContent();

        $this->deleteJson("/api/configuracoes/tokens/{$criado['id']}", [], $headers)
            ->assertNotFound();
    }

    public function test_configuracoes_exigem_admin(): void
    {
        $this->getJson('/api/configuracoes/sistema')->assertUnauthorized();
        $this->getJson('/api/configuracoes/tokens')->assertUnauthorized();

        $bolsista = $this->criarLogin();
        $this->comPermissao($bolsista, Role::BOLSISTA);
        $headers = $this->authHeader($bolsista, Role::BOLSISTA);

        $this->getJson('/api/configuracoes/sistema', $headers)->assertForbidden();
        $this->putJson('/api/configuracoes/sistema', [], $headers)->assertForbidden();
        $this->getJson('/api/configuracoes/tokens', $headers)->assertForbidden();
        $this->postJson('/api/configuracoes/tokens', [], $headers)->assertForbidden();
    }
}
