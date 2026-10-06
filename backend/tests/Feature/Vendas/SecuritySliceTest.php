<?php

namespace Tests\Feature\Vendas;

use App\Modules\Auth\Enums\Role;

/** Slice 401/403 por rota do Vendas (escrita Admin, leitura sem Recrutando). */
class SecuritySliceTest extends VendasTestCase
{
    /** @return list<array{0:string,1:string}> */
    private static function rotasEscritaAdmin(): array
    {
        return [
            ['POST', '/api/vendas/clientes'],
            ['PUT', '/api/vendas/clientes/1'],
            ['DELETE', '/api/vendas/clientes/1'],
            ['POST', '/api/vendas/clientes/1/tags'],
            ['DELETE', '/api/vendas/clientes/1/tags/1'],
            ['POST', '/api/vendas/clientes/bulk-tag'],
            ['POST', '/api/vendas/tags'],
            ['POST', '/api/vendas/orcamentos'],
            ['PUT', '/api/vendas/orcamentos/1'],
            ['POST', '/api/vendas/orcamentos/1/duplicar'],
            ['POST', '/api/vendas/encomendas'],
            ['POST', '/api/vendas/marketplace'],
            ['POST', '/api/vendas/interacoes'],
            ['POST', '/api/vendas/tarefas-marketing'],
            ['PUT', '/api/vendas/tarefas-marketing/1'],
            ['PUT', '/api/vendas/solicitacoes/1/decisao'],
        ];
    }

    /** @return list<string> */
    private static function rotasLeitura(): array
    {
        return [
            '/api/vendas/clientes',
            '/api/vendas/clientes/1',
            '/api/vendas/tags',
            '/api/vendas/orcamentos',
            '/api/vendas/orcamentos/1',
            '/api/vendas/encomendas',
            '/api/vendas/encomendas/1',
            '/api/vendas/encomendas/1/status',
            '/api/vendas/historico-status/1',
            '/api/vendas/marketplace',
            '/api/vendas/interacoes/1',
            '/api/vendas/tarefas-marketing',
            '/api/vendas/solicitacoes',
        ];
    }

    public function test_escrita_sem_token_da_401(): void
    {
        foreach (self::rotasEscritaAdmin() as [$verbo, $uri]) {
            $res = match ($verbo) {
                'POST' => $this->postJson($uri),
                'PUT' => $this->putJson($uri),
                default => $this->deleteJson($uri),
            };
            $res->assertUnauthorized($uri);
        }

        $this->postJson('/api/vendas/solicitacoes')->assertUnauthorized();
        $this->putJson('/api/vendas/encomendas/1/kanban')->assertUnauthorized();
        $this->postJson('/api/vendas/encomendas/1/nova-ordem')->assertUnauthorized();
    }

    public function test_leitura_sem_token_da_401(): void
    {
        foreach (self::rotasLeitura() as $uri) {
            $this->getJson($uri)->assertUnauthorized($uri);
        }
    }

    public function test_recrutando_sem_acesso_ao_modulo(): void
    {
        $headers = $this->headersPapel(Role::RECRUTANDO);

        foreach (self::rotasLeitura() as $uri) {
            $this->getJson($uri, $headers)->assertForbidden($uri);
        }

        $this->postJson('/api/vendas/clientes', $this->clientePayload(), $headers)->assertForbidden();
        // Payload válido: a negação vem da policy (403), não da validação.
        $this->postJson('/api/vendas/solicitacoes', [
            'tipo' => 'OUTRA', 'alvoTipo' => 'CLIENTE', 'alvoId' => 1,
            'campo' => 'c', 'valorProposto' => 'v', 'justificativa' => 'j',
        ], $headers)->assertForbidden();
    }

    public function test_bolsista_voluntario_estagiario_nao_escrevem(): void
    {
        foreach ([Role::BOLSISTA, Role::VOLUNTARIO, Role::ESTAGIARIO] as $role) {
            $headers = $this->headersPapel($role);

            // Leitura passa (exceto detalhe inexistente → 404, ainda autenticado).
            $this->getJson('/api/vendas/clientes', $headers)->assertOk($role->name);
            $this->getJson('/api/vendas/tags', $headers)->assertOk($role->name);

            // Escrita de domínio → 403.
            $this->postJson('/api/vendas/clientes', $this->clientePayload(), $headers)
                ->assertForbidden($role->name.' clientes store');
            $this->postJson('/api/vendas/tags', ['nome' => 'X'], $headers)
                ->assertForbidden($role->name.' tags store');
            $this->putJson('/api/vendas/solicitacoes/1/decisao', ['decisao' => 'APROVAR'], $headers)
                ->assertForbidden($role->name.' decidir');

            // Pedir solicitação passa (B/V/E autenticados).
            $this->postJson('/api/vendas/solicitacoes', [
                'tipo' => 'OUTRA', 'alvoTipo' => 'CLIENTE', 'alvoId' => 1,
                'campo' => 'c', 'valorProposto' => 'v', 'justificativa' => 'j',
            ], $headers)->assertCreated($role->name.' solicitar');
        }
    }

    public function test_kanban_nova_ordem_sem_token_ou_papel_da_401_403(): void
    {
        $headersAdmin = $this->headersAdmin();
        $clienteId = $this->postJson('/api/vendas/clientes', $this->clientePayload(), $headersAdmin)
            ->assertCreated()->json('id');
        $orcId = $this->postJson('/api/vendas/orcamentos', $this->orcamentoPayload($clienteId), $headersAdmin)
            ->assertCreated()->json('id');
        $this->putJson("/api/vendas/orcamentos/{$orcId}", ['status' => 'Aprovado'], $headersAdmin)->assertOk();
        $id = $this->postJson('/api/vendas/encomendas', ['idOrcamento' => $orcId], $headersAdmin)
            ->assertCreated()->json('id');

        // Não-criador não-Admin (Bolsista) → 403 mesmo com payload válido.
        $headersB = $this->headersPapel(Role::BOLSISTA);
        $this->putJson("/api/vendas/encomendas/{$id}/kanban", [
            'statusKanban' => 'Produção', 'versao' => 0,
        ], $headersB)->assertForbidden();
        $this->postJson("/api/vendas/encomendas/{$id}/nova-ordem", [
            'valorFinal' => '10.00',
        ], $headersB)->assertForbidden();

        // Criador (Admin) move normalmente.
        $this->putJson("/api/vendas/encomendas/{$id}/kanban", [
            'statusKanban' => 'Produção', 'versao' => 0,
        ], $headersAdmin)->assertOk();
    }
}
