<?php

namespace Tests\Feature\Producao;

use App\Modules\Auth\Enums\Role;
use App\Modules\Producao\Models\SetorResponsavel;

/** Setores: vínculo de responsável ativo, checklist mínimo, rotação. */
class SetoresTest extends ProducaoTestCase
{
    private function setorPayload(): array
    {
        return ['numero' => 3, 'nome' => 'Eletrônica'];
    }

    public function test_criar_exige_admin_ou_vinculo(): void
    {
        $admin = $this->headersAdmin();
        $id = $this->postJson('/api/producao/setores', $this->setorPayload(), $admin)
            ->assertCreated()->assertJsonPath('ativo', true)->json('id');

        // Bolsista sem vínculo → 403.
        $bolsista = $this->loginPapel(Role::BOLSISTA);
        $headersB = $this->authHeader($bolsista, Role::BOLSISTA);
        $this->postJson('/api/producao/setores', $this->setorPayload(), $headersB)->assertForbidden();

        // Designação por Bolsista → 403 (delta Admin-only); Admin designa.
        $this->postJson("/api/producao/setores/{$id}/responsaveis", [
            'idFuncionario' => $bolsista->id_user, 'dataInicio' => today()->toDateString(),
        ], $headersB)->assertForbidden();
        $this->postJson("/api/producao/setores/{$id}/responsaveis", [
            'idFuncionario' => $bolsista->id_user, 'dataInicio' => today()->toDateString(),
        ], $admin)->assertCreated();

        // Com vínculo, cria e edita; sem vínculo em outro setor, não edita.
        $this->postJson('/api/producao/setores', $this->setorPayload(), $headersB)->assertCreated();
        $id2 = $this->postJson('/api/producao/setores', $this->setorPayload(), $admin)->assertCreated()->json('id');
        $this->putJson("/api/producao/setores/{$id2}", array_merge($this->setorPayload(), ['nome' => 'X']), $headersB)
            ->assertForbidden();
        $this->putJson("/api/producao/setores/{$id}", array_merge($this->setorPayload(), ['nome' => 'Eletro']), $headersB)
            ->assertOk()->assertJsonPath('nome', 'Eletro');
    }

    public function test_materiais_sinalizacoes_checklist(): void
    {
        $admin = $this->headersAdmin();
        $id = $this->postJson('/api/producao/setores', $this->setorPayload(), $admin)->assertCreated()->json('id');

        $this->postJson("/api/producao/setores/{$id}/materiais",
            ['descricao' => 'Estanho', 'quantidade' => '2.50'], $admin)
            ->assertCreated()->assertJsonPath('quantidade', '2.50');

        $this->postJson("/api/producao/setores/{$id}/sinalizacoes", ['texto' => 'Use EPI'], $admin)
            ->assertCreated()->assertJsonPath('texto', 'Use EPI');

        $item = $this->postJson("/api/producao/setores/{$id}/checklist", ['item' => 'Bancada limpa'], $admin)
            ->assertCreated()->assertJsonPath('ativo', true)->json();

        // Update edita só item+ativo (sem peso/ordem no contrato).
        $this->putJson("/api/producao/setores/{$id}/checklist/{$item['id']}",
            ['item' => 'Bancada limpa e organizada', 'ativo' => false], $admin)
            ->assertOk()->assertJsonPath('item', 'Bancada limpa e organizada')->assertJsonPath('ativo', false);

        $this->getJson("/api/producao/setores/{$id}", $admin)->assertOk()
            ->assertJsonCount(1, 'materiais')->assertJsonCount(1, 'sinalizacoes')->assertJsonCount(1, 'checklists');

        $this->deleteJson("/api/producao/setores/{$id}/checklist/{$item['id']}", [], $admin)->assertNoContent();
        $this->deleteJson("/api/producao/setores/{$id}/materiais/1", [], $admin)->assertNoContent();
        $this->deleteJson("/api/producao/setores/{$id}/sinalizacoes/1", [], $admin)->assertNoContent();
    }

    public function test_rotacao_desativa_anterior_e_filtros(): void
    {
        $admin = $this->headersAdmin();
        $id = $this->postJson('/api/producao/setores', $this->setorPayload(), $admin)->assertCreated()->json('id');

        $r1 = $this->postJson("/api/producao/setores/{$id}/responsaveis", [
            'idFuncionario' => 51, 'dataInicio' => today()->toDateString(),
        ], $admin)->assertCreated()->json();
        $r2 = $this->postJson("/api/producao/setores/{$id}/responsaveis", [
            'idFuncionario' => 52, 'dataInicio' => today()->toDateString(),
        ], $admin)->assertCreated()->json();

        $this->assertFalse((bool) SetorResponsavel::find($r1['id'])->ativo);
        $this->assertTrue((bool) SetorResponsavel::find($r2['id'])->ativo);

        $this->getJson("/api/producao/setores/{$id}/responsaveis", $admin)->assertOk()->assertJsonCount(2);
        $this->getJson('/api/producao/setores?ativo=1', $admin)->assertOk()->assertJsonCount(1);

        $this->deleteJson("/api/producao/setores/{$id}/responsaveis/{$r1['id']}", [], $admin)->assertNoContent();
        $this->deleteJson("/api/producao/setores/{$id}", [], $admin)->assertNoContent();
    }
}
