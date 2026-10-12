<?php

namespace Tests\Feature\Financeiro;

use App\Modules\Financeiro\Models\CustoEncomenda;
use Carbon\CarbonImmutable;

/** 6 relatórios contra fixtures + allowlist de período. */
class RelatoriosTest extends FinanceiroTestCase
{
    private function fixtures(array $headers): array
    {
        $catId = $this->criarCategoria()->getKey();
        $post = fn (array $payload) => $this->postJson(
            '/api/financeiro/lancamentos', $this->lancamentoPayload($catId, $payload), $headers,
        )->assertCreated()->json('id');

        $e1 = $post(['tipo' => 'ENTRADA', 'valor' => '1000.00', 'dataVencimento' => '2026-03-05']);
        $e2 = $post(['tipo' => 'ENTRADA', 'valor' => '500.00', 'dataVencimento' => '2026-03-12']);
        $post(['valor' => '300.00', 'dataVencimento' => '2026-03-06', 'idReferenciaExterna' => 'MAQ-1']);
        $post(['valor' => '200.00', 'dataVencimento' => '2026-03-13', 'idReferenciaExterna' => 'MAQ-1']);
        $post(['valor' => '100.00', 'dataVencimento' => '2026-04-02', 'idReferenciaExterna' => 'MAQ-2']);

        // Cancelado fora das somas (sem rota de cancel — insert direto, como legado).
        $cancel = \App\Modules\Financeiro\Models\LancamentoFinanceiro::create([
            'id_categoria' => $catId, 'tipo' => 'SAIDA', 'valor' => '999.00',
            'data_vencimento' => '2026-03-07', 'status' => 'CANCELADO',
        ]);

        // E1 pago (some da inadimplência); E2 segue ATRASADO.
        $this->putJson("/api/financeiro/lancamentos/{$e1}/pagamento", [], $headers)->assertOk();

        $this->postJson('/api/financeiro/doacoes-recursos', [
            'tipo' => 'DOACAO', 'origem' => 'Doador', 'valor' => '400.00',
            'dataRecebimento' => '2026-03-03',
        ], $headers)->assertCreated();
        $this->postJson('/api/financeiro/doacoes-recursos', [
            'tipo' => 'PROJETO', 'origem' => 'Edital', 'valor' => '600.00',
            'dataRecebimento' => '2026-03-15',
        ], $headers)->assertCreated();

        return ['e2' => $e2, 'cancel' => $cancel->getKey()];
    }

    public function test_fluxo_caixa_com_semanas(): void
    {
        $headers = $this->headersAdmin();
        $this->fixtures($headers);

        $this->getJson('/api/financeiro/relatorios/fluxo-caixa?periodo=2026-03', $headers)->assertOk()
            ->assertJsonPath('entradas', '1500.00')
            ->assertJsonPath('saidas', '500.00')
            ->assertJsonPath('saldo', '1000.00')
            ->assertJsonPath('semanas', [
                ['semana' => '2026-S10', 'entradas' => '1000.00', 'saidas' => '300.00', 'liquido' => '700.00'],
                ['semana' => '2026-S11', 'entradas' => '500.00', 'saidas' => '200.00', 'liquido' => '300.00'],
            ]);
    }

    public function test_dre_soma_doacoes_e_zera_diretos(): void
    {
        $headers = $this->headersAdmin();
        $this->fixtures($headers);

        $this->getJson('/api/financeiro/relatorios/dre?periodo=2026-03', $headers)->assertOk()
            ->assertJson([
                'receitaOperacional' => '1500.00',
                'custosDiretos' => '0.00',
                'maoDeObra' => '0.00',
                'overhead' => '0.00',
                'despesasOperacionais' => '500.00',
                'doacoesRecursos' => '1000.00',
                'resultado' => '2000.00',
            ]);
    }

    public function test_inadimplencia_so_entrada_atrasado(): void
    {
        $headers = $this->headersAdmin();
        ['e2' => $e2] = $this->fixtures($headers);

        $dias = (int) CarbonImmutable::parse('2026-03-12')->diffInDays(CarbonImmutable::parse(today()->toDateString()));

        $this->getJson('/api/financeiro/relatorios/inadimplencia', $headers)->assertOk()
            ->assertJsonCount(1)
            ->assertJsonPath('0.idLancamento', $e2)
            ->assertJsonPath('0.valor', '500.00')
            ->assertJsonPath('0.diasEmAtraso', $dias);
    }

    public function test_doacoes_despesas_com_cobertura_e_meses(): void
    {
        $headers = $this->headersAdmin();
        $this->fixtures($headers);

        $this->getJson('/api/financeiro/relatorios/doacoes-despesas?periodo=2026-03', $headers)->assertOk()
            ->assertJsonPath('doacoes', '400.00')
            ->assertJsonPath('recursosProjeto', '600.00')
            ->assertJsonPath('despesas', '500.00')
            ->assertJsonPath('saldo', '500.00')
            ->assertJsonPath('coberturaPercentual', '200.00')
            ->assertJsonPath('meses', [
                ['mes' => '2026-03', 'doacoes' => '1000.00', 'despesas' => '500.00', 'saldo' => '500.00'],
            ]);
    }

    public function test_lucratividade_ultimo_por_encomenda_com_margem_pct(): void
    {
        $headers = $this->headersAdmin();

        foreach ([
            ['id_encomenda' => 21, 'valor' => '1000.00', 'total' => '700.00', 'margem' => '300.00', 'data' => '2026-01-01'],
            ['id_encomenda' => 21, 'valor' => '1000.00', 'total' => '800.00', 'margem' => '200.00', 'data' => '2026-02-01'],
            ['id_encomenda' => 22, 'valor' => '500.00', 'total' => '600.00', 'margem' => '-100.00', 'data' => '2026-02-01'],
        ] as $row) {
            CustoEncomenda::create([
                'id_encomenda' => $row['id_encomenda'],
                'custo_materiais' => $row['total'], 'custo_mao_obra' => '0.00', 'custo_overhead' => '0.00',
                'custo_total' => $row['total'], 'valor_venda' => $row['valor'],
                'margem_lucro' => $row['margem'], 'data_calculo' => $row['data'],
            ]);
        }

        $this->getJson('/api/financeiro/relatorios/lucratividade', $headers)->assertOk()
            ->assertJson([
                ['idEncomenda' => 21, 'valorVenda' => '1000.00', 'custoTotal' => '800.00',
                    'margem' => '200.00', 'margemPercentual' => '20.00'],
                ['idEncomenda' => 22, 'valorVenda' => '500.00', 'custoTotal' => '600.00',
                    'margem' => '-100.00', 'margemPercentual' => '-20.00'],
            ]);
    }

    public function test_custo_maquina_agrega_por_referencia(): void
    {
        $headers = $this->headersAdmin();
        $this->fixtures($headers);

        $this->getJson('/api/financeiro/relatorios/custo-maquina', $headers)->assertOk()
            ->assertJson([
                ['idMaquina' => 'MAQ-1', 'custo' => '500.00', 'percentualTotal' => '83.33'],
                ['idMaquina' => 'MAQ-2', 'custo' => '100.00', 'percentualTotal' => '16.67'],
            ]);
    }

    public function test_periodo_invalido_422_e_data_tem_precedencia(): void
    {
        $headers = $this->headersAdmin();
        $this->fixtures($headers);

        foreach (['fluxo-caixa', 'dre', 'doacoes-despesas'] as $rel) {
            $this->getJson("/api/financeiro/relatorios/{$rel}?periodo=2026-13", $headers)->assertUnprocessable();
            $this->getJson("/api/financeiro/relatorios/{$rel}?periodo=hoje", $headers)->assertUnprocessable();
        }

        // dataInicio/dataFim vencem o periodo (só S3 em abril).
        $this->getJson(
            '/api/financeiro/relatorios/fluxo-caixa?dataInicio=2026-04-01&dataFim=2026-04-30&periodo=2026-03',
            $headers,
        )->assertOk()
            ->assertJsonPath('entradas', '0.00')
            ->assertJsonPath('saidas', '100.00');
    }
}
