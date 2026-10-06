<?php

namespace Tests\Feature\Vendas;

use App\Modules\Auth\Enums\Role;
use App\Modules\Vendas\Models\Cliente;

/** Clientes/tags: CRUD, filtros, vínculo, bulk-tag, guarda de exclusão, LGPD. */
class ClientesTest extends VendasTestCase
{
    public function test_criar_cliente_mascara_documento(): void
    {
        $res = $this->postJson('/api/vendas/clientes', $this->clientePayload(), $this->headersAdmin());

        $res->assertCreated()
            ->assertJsonPath('tipoPessoa', 'PF')
            ->assertJsonPath('documento', '***.982.247-**');

        $this->assertSame(self::CPF_VALIDO, Cliente::first()->cpf_cnpj);
    }

    public function test_criar_cliente_cnpj(): void
    {
        $res = $this->postJson(
            '/api/vendas/clientes',
            $this->clientePayload(['tipoPessoa' => 'PJ', 'cpfCnpj' => self::CNPJ_VALIDO]),
            $this->headersAdmin(),
        );

        $res->assertCreated()->assertJsonPath('documento', '**. 444.777/0001-**');
    }

    public function test_documento_invaluido_duplicado_rejeitados(): void
    {
        $headers = $this->headersAdmin();

        $this->postJson('/api/vendas/clientes', $this->clientePayload(['cpfCnpj' => '123']), $headers)
            ->assertStatus(422);

        $this->postJson('/api/vendas/clientes', $this->clientePayload(), $headers)->assertCreated();
        $this->postJson('/api/vendas/clientes', $this->clientePayload(), $headers)->assertConflict();
    }

    public function test_listar_filtros_termo_tipo_tags(): void
    {
        $headers = $this->headersAdmin();
        $tag = $this->criarTag(['nome' => 'VIP']);

        $idA = $this->postJson('/api/vendas/clientes', $this->clientePayload([
            'nomeRazaoSocial' => 'Ana Corte',
        ]), $headers)->assertCreated()->json('id');
        $this->postJson("/api/vendas/clientes/{$idA}/tags", ['tagId' => $tag->getKey()], $headers)
            ->assertCreated();

        // Segundo cliente precisa de outro documento: usa CNPJ.
        $this->postJson('/api/vendas/clientes', $this->clientePayload([
            'nomeRazaoSocial' => 'Beta PJ', 'tipoPessoa' => 'PJ', 'cpfCnpj' => self::CNPJ_VALIDO,
        ]), $headers)->assertCreated();

        $this->getJson('/api/vendas/clientes?termo=Ana', $headers)
            ->assertOk()->assertJsonCount(1, 'clientes');

        $this->getJson('/api/vendas/clientes?tipo=PJ', $headers)
            ->assertOk()->assertJsonCount(1, 'clientes');

        $this->getJson("/api/vendas/clientes?tags[]={$tag->getKey()}", $headers)
            ->assertOk()->assertJsonCount(1, 'clientes');
    }

    public function test_detalhe_traz_indicadores(): void
    {
        $headers = $this->headersAdmin();
        $id = $this->postJson('/api/vendas/clientes', $this->clientePayload(), $headers)
            ->assertCreated()->json('id');

        $this->getJson("/api/vendas/clientes/{$id}", $headers)
            ->assertOk()
            ->assertJsonPath('indicadores.orcamentosEmAberto', 0)
            ->assertJsonPath('indicadores.encomendasEmProducao', 0)
            ->assertJsonPath('indicadores.ultimaInteracao', null);
    }

    public function test_vincular_desvincular_tag_idempotente(): void
    {
        $headers = $this->headersAdmin();
        $id = $this->postJson('/api/vendas/clientes', $this->clientePayload(), $headers)
            ->assertCreated()->json('id');
        $tagId = $this->criarTag()->getKey();

        $this->postJson("/api/vendas/clientes/{$id}/tags", ['tagId' => $tagId], $headers)->assertCreated();
        $this->postJson("/api/vendas/clientes/{$id}/tags", ['tagId' => $tagId], $headers)->assertCreated();

        $this->assertSame(1, Cliente::find($id)->tags()->count());

        $this->deleteJson("/api/vendas/clientes/{$id}/tags/{$tagId}", [], $headers)->assertNoContent();
        $this->assertSame(0, Cliente::find($id)->tags()->count());
    }

    public function test_bulk_tag_admin_e_contagem_sem_duplicar(): void
    {
        $headers = $this->headersAdmin();
        $idA = $this->postJson('/api/vendas/clientes', $this->clientePayload(), $headers)
            ->assertCreated()->json('id');
        $idB = $this->postJson('/api/vendas/clientes', $this->clientePayload([
            'cpfCnpj' => self::CNPJ_VALIDO, 'tipoPessoa' => 'PJ',
        ]), $headers)->assertCreated()->json('id');
        $tagId = $this->criarTag()->getKey();

        $this->postJson('/api/vendas/clientes/bulk-tag', [
            'clienteIds' => [$idA, $idB], 'tagId' => $tagId,
        ], $headers)->assertOk()->assertJsonPath('vinculados', 2);

        $this->postJson('/api/vendas/clientes/bulk-tag', [
            'clienteIds' => [$idA, $idB], 'tagId' => $tagId,
        ], $headers)->assertOk()->assertJsonPath('vinculados', 0);
    }

    public function test_bulk_tag_nao_admin_403(): void
    {
        $this->postJson('/api/vendas/clientes/bulk-tag', [
            'clienteIds' => [1], 'tagId' => 1,
        ], $this->headersPapel(Role::BOLSISTA))->assertForbidden();
    }

    public function test_excluir_bloqueado_com_encomenda_vinculada(): void
    {
        $headers = $this->headersAdmin();
        $clienteId = $this->postJson('/api/vendas/clientes', $this->clientePayload(), $headers)
            ->assertCreated()->json('id');

        $orcId = $this->postJson('/api/vendas/orcamentos', $this->orcamentoPayload($clienteId), $headers)
            ->assertCreated()->json('id');
        $this->putJson("/api/vendas/orcamentos/{$orcId}", ['status' => 'Aprovado'], $headers)->assertOk();
        $this->postJson('/api/vendas/encomendas', ['idOrcamento' => $orcId], $headers)->assertCreated();

        $this->deleteJson("/api/vendas/clientes/{$clienteId}", [], $headers)->assertConflict();
        $this->assertNotNull(Cliente::find($clienteId));
    }

    public function test_tags_crud(): void
    {
        $headers = $this->headersAdmin();

        $this->postJson('/api/vendas/tags', ['nome' => 'VIP', 'cor' => '#f00'], $headers)
            ->assertCreated()->assertJsonPath('nome', 'VIP');
        $this->postJson('/api/vendas/tags', ['nome' => 'VIP'], $headers)->assertConflict();

        $this->getJson('/api/vendas/tags', $headers)->assertOk()->assertJsonCount(1);
    }
}
