<?php

namespace Tests\Feature\Financeiro;

use App\Modules\Auth\Enums\Role;

/** Slice 401/403 do Financeiro: tudo Admin (401 sem token, 403 com role ≠ 0). */
class SecuritySliceTest extends FinanceiroTestCase
{
    /** @return list<string> */
    private static function rotasGet(): array
    {
        return [
            '/api/financeiro/categorias',
            '/api/financeiro/lancamentos',
            '/api/financeiro/doacoes-recursos',
            '/api/financeiro/solicitacoes-compra',
            '/api/financeiro/solicitacoes-compra/1',
            '/api/financeiro/valores-hora',
            '/api/financeiro/parametros-overhead',
            '/api/financeiro/fechamento-encomenda',
            '/api/financeiro/fechamento-encomenda/1',
            '/api/financeiro/custos-encomenda/1',
            '/api/financeiro/relatorios/fluxo-caixa',
            '/api/financeiro/relatorios/dre',
            '/api/financeiro/relatorios/lucratividade',
            '/api/financeiro/relatorios/inadimplencia',
            '/api/financeiro/relatorios/doacoes-despesas',
            '/api/financeiro/relatorios/custo-maquina',
        ];
    }

    /** @return list<array{0:string,1:string}> */
    private static function rotasEscrita(): array
    {
        return [
            ['POST', '/api/financeiro/categorias'],
            ['POST', '/api/financeiro/lancamentos'],
            ['PUT', '/api/financeiro/lancamentos/1/pagamento'],
            ['POST', '/api/financeiro/doacoes-recursos'],
            ['POST', '/api/financeiro/solicitacoes-compra'],
            ['PUT', '/api/financeiro/solicitacoes-compra/1/concluir'],
            ['POST', '/api/financeiro/valores-hora'],
            ['POST', '/api/financeiro/parametros-overhead'],
            ['POST', '/api/financeiro/fechamento-encomenda'],
            ['POST', '/api/financeiro/fechamento-encomenda/1/nova-ordem'],
        ];
    }

    public function test_sem_token_da_401(): void
    {
        foreach (self::rotasGet() as $uri) {
            $this->getJson($uri)->assertUnauthorized($uri);
        }

        foreach (self::rotasEscrita() as [$verbo, $uri]) {
            $res = $verbo === 'POST' ? $this->postJson($uri) : $this->putJson($uri);
            $res->assertUnauthorized($uri);
        }
    }

    public function test_papel_nao_admin_da_403(): void
    {
        foreach ([Role::BOLSISTA, Role::VOLUNTARIO, Role::ESTAGIARIO, Role::RECRUTANDO] as $role) {
            $headers = $this->headersPapel($role);

            foreach (self::rotasGet() as $uri) {
                $this->getJson($uri, $headers)->assertForbidden($role->name.' '.$uri);
            }

            // O `can:admin` nega antes da validação: 403 mesmo sem payload.
            foreach (self::rotasEscrita() as [$verbo, $uri]) {
                $res = $verbo === 'POST' ? $this->postJson($uri, [], $headers) : $this->putJson($uri, [], $headers);
                $res->assertForbidden($role->name.' '.$verbo.' '.$uri);
            }

            // Payload válido também nega (a negação vem do Gate, não da validação).
            $this->postJson('/api/financeiro/categorias', ['nome' => 'X', 'tipo' => 'DESPESA'], $headers)
                ->assertForbidden($role->name.' categorias store');
        }
    }

    public function test_admin_passa_no_gate(): void
    {
        $headers = $this->headersAdmin();

        $this->getJson('/api/financeiro/categorias', $headers)->assertOk();
        $this->getJson('/api/financeiro/relatorios/fluxo-caixa', $headers)->assertOk();
    }
}
