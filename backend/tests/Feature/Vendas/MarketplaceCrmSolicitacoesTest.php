<?php

namespace Tests\Feature\Vendas;

use App\Modules\Auth\Enums\Role;
use App\Modules\Vendas\Models\Cliente;

/** Marketplace, CRM e solicitações: registro, totais, escopo, D-4, counts. */
class MarketplaceCrmSolicitacoesTest extends VendasTestCase
{
    private function encomendaPronta(array $headers): int
    {
        $clienteId = $this->postJson('/api/vendas/clientes', $this->clientePayload(), $headers)
            ->assertCreated()->json('id');
        $orcId = $this->postJson('/api/vendas/orcamentos', $this->orcamentoPayload($clienteId), $headers)
            ->assertCreated()->json('id');
        $this->putJson("/api/vendas/orcamentos/{$orcId}", ['status' => 'Aprovado'], $headers)->assertOk();

        return $this->postJson('/api/vendas/encomendas', ['idOrcamento' => $orcId], $headers)
            ->assertCreated()->json('id');
    }

    public function test_marketplace_registra_totais_e_liquido(): void
    {
        $headers = $this->headersAdmin();
        $id = $this->encomendaPronta($headers);

        $res = $this->postJson('/api/vendas/marketplace', [
            'encomendaId' => $id,
            'plataforma' => 'Shopee',
            'codigoExterno' => 'SP-1',
            'dataVenda' => today()->toDateString(),
            'valorTaxa' => '12.00',
        ], $headers);

        $res->assertCreated()
            ->assertJsonPath('codigo', "EN-{$id}")
            ->assertJsonPath('valorBruto', '100.00')
            ->assertJsonPath('valorLiquido', '88.00');

        $this->postJson('/api/vendas/marketplace', [
            'encomendaId' => $id,
            'plataforma' => 'Shopee',
            'codigoExterno' => 'SP-1',
            'dataVenda' => today()->toDateString(),
            'valorTaxa' => '1.00',
        ], $headers)->assertConflict();

        $this->getJson('/api/vendas/marketplace?plataforma=Shopee', $headers)
            ->assertOk()
            ->assertJsonPath('totais.bruto', '100.00')
            ->assertJsonPath('totais.taxas', '12.00')
            ->assertJsonPath('totais.liquido', '88.00');

        $this->getJson("/api/vendas/marketplace?encomendaId={$id}", $headers)
            ->assertOk()->assertJsonCount(1, 'registros');
    }

    public function test_marketplace_encomenda_inexistente_404(): void
    {
        $this->postJson('/api/vendas/marketplace', [
            'encomendaId' => 999999,
            'plataforma' => 'ML',
            'codigoExterno' => 'X',
            'dataVenda' => today()->toDateString(),
            'valorTaxa' => '1.00',
        ], $this->headersAdmin())->assertNotFound();
    }

    public function test_interacoes_escopadas_por_cliente(): void
    {
        $headers = $this->headersAdmin();
        $clienteId = $this->postJson('/api/vendas/clientes', $this->clientePayload(), $headers)
            ->assertCreated()->json('id');

        $this->postJson('/api/vendas/interacoes', [
            'clienteId' => $clienteId, 'tipo' => 'E-mail', 'descricao' => 'Enviou orçamento',
        ], $headers)->assertCreated()->assertJsonPath('tipo', 'E-mail');

        $this->postJson('/api/vendas/interacoes', [
            'clienteId' => $clienteId, 'tipo' => 'SMS', 'descricao' => 'x',
        ], $headers)->assertStatus(422);

        $this->getJson("/api/vendas/interacoes/{$clienteId}", $headers)
            ->assertOk()->assertJsonCount(1);

        $this->getJson('/api/vendas/interacoes/999999', $headers)->assertNotFound();
    }

    public function test_tarefas_criar_listar_atualizar_e_prazo(): void
    {
        $headers = $this->headersAdmin();

        $id = $this->postJson('/api/vendas/tarefas-marketing', [
            'titulo' => 'Posts', 'responsavelId' => 1001,
            'dataInicio' => today()->toDateString(),
            'dataFim' => today()->addWeek()->toDateString(),
        ], $headers)->assertCreated()
            ->assertJsonPath('status', 'Pendente')
            ->assertJsonPath('prioridade', 'Média')
            ->json('id');

        $this->postJson('/api/vendas/tarefas-marketing', [
            'titulo' => 'Ruim', 'responsavelId' => 1001,
            'dataInicio' => today()->addWeek()->toDateString(),
            'dataFim' => today()->toDateString(),
        ], $headers)->assertBadRequest();

        $this->putJson("/api/vendas/tarefas-marketing/{$id}", ['status' => 'Em Andamento'], $headers)
            ->assertOk()->assertJsonPath('status', 'Em Andamento');

        $this->getJson('/api/vendas/tarefas-marketing?status=Em Andamento', $headers)
            ->assertOk()->assertJsonCount(1, 'tarefas')
            ->assertJsonPath('counts.Em Andamento', 1);
    }

    public function test_solicitar_decidir_d4_sem_auto_apply(): void
    {
        $headersAdmin = $this->headersAdmin();
        $clienteId = $this->postJson('/api/vendas/clientes', $this->clientePayload([
            'nomeRazaoSocial' => 'Nome Original',
        ]), $headersAdmin)->assertCreated()->json('id');

        // Bolsista pode pedir (não decidir).
        $headersBolsista = $this->headersPapel(Role::BOLSISTA);
        $solId = $this->postJson('/api/vendas/solicitacoes', [
            'tipo' => 'ALTERACAO_DADOS',
            'alvoTipo' => 'CLIENTE',
            'alvoId' => $clienteId,
            'campo' => 'nome_razao_social',
            'valorAtual' => 'Nome Original',
            'valorProposto' => 'Nome Novo',
            'justificativa' => 'Correção',
        ], $headersBolsista)->assertCreated()
            ->assertJsonPath('status', 'Pendente')
            ->json('id');

        $this->putJson("/api/vendas/solicitacoes/{$solId}/decisao", ['decisao' => 'APROVAR'], $headersBolsista)
            ->assertForbidden();

        $this->putJson("/api/vendas/solicitacoes/{$solId}/decisao", ['decisao' => 'APROVAR'], $headersAdmin)
            ->assertOk()->assertJsonPath('status', 'Aprovada');

        // D-4: aprovada NÃO altera o alvo.
        $this->assertSame('Nome Original', Cliente::find($clienteId)->nome_razao_social);

        $this->putJson("/api/vendas/solicitacoes/{$solId}/decisao", ['decisao' => 'APROVAR'], $headersAdmin)
            ->assertConflict();

        // Rejeitar exige motivo.
        $sol2 = $this->postJson('/api/vendas/solicitacoes', [
            'tipo' => 'OUTRA', 'alvoTipo' => 'CLIENTE', 'alvoId' => $clienteId,
            'campo' => 'email', 'valorProposto' => 'x@y.z', 'justificativa' => 'j',
        ], $headersBolsista)->assertCreated()->json('id');

        $this->putJson("/api/vendas/solicitacoes/{$sol2}/decisao", ['decisao' => 'REJEITAR'], $headersAdmin)
            ->assertBadRequest();
        $this->putJson("/api/vendas/solicitacoes/{$sol2}/decisao", [
            'decisao' => 'REJEITAR', 'motivo' => 'Sem necessidade',
        ], $headersAdmin)->assertOk()->assertJsonPath('status', 'Rejeitada');

        $this->getJson('/api/vendas/solicitacoes?status=Aprovada', $headersAdmin)
            ->assertOk()->assertJsonCount(1, 'solicitacoes')
            ->assertJsonPath('counts.Aprovada', 1)
            ->assertJsonPath('counts.Rejeitada', 1);
    }

    public function test_recrutando_nao_solicita(): void
    {
        $this->postJson('/api/vendas/solicitacoes', [
            'tipo' => 'OUTRA', 'alvoTipo' => 'CLIENTE', 'alvoId' => 1,
            'campo' => 'c', 'valorProposto' => 'v', 'justificativa' => 'j',
        ], $this->headersPapel(Role::RECRUTANDO))->assertForbidden();
    }
}
