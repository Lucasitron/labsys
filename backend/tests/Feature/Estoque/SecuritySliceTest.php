<?php

namespace Tests\Feature\Estoque;

use App\Modules\Auth\Enums\Role;

/** Slice 401/403 por rota do Estoque (incl. vínculo e Recrutando). */
class SecuritySliceTest extends EstoqueTestCase
{
    /** @return list<array{0:string,1:string,2:array}> */
    private static function rotasEscrita(): array
    {
        return [
            ['POST', '/api/estoque/itens', []],
            ['POST', '/api/estoque/entradas', []],
            ['POST', '/api/estoque/saidas', []],
            ['POST', '/api/estoque/emprestimos', []],
            ['POST', '/api/estoque/fornecedores', []],
            ['POST', '/api/estoque/localizacoes', []],
            ['POST', '/api/estoque/boms', []],
        ];
    }

    public function test_escrita_sem_token_da_401(): void
    {
        foreach (self::rotasEscrita() as [$verbo, $uri]) {
            $res = $verbo === 'POST' ? $this->postJson($uri) : $this->getJson($uri);
            $res->assertUnauthorized($uri);
        }
    }

    public function test_leitura_sem_token_da_401(): void
    {
        $this->getJson('/api/estoque/itens')->assertUnauthorized();
        $this->getJson('/api/estoque/emprestimos')->assertUnauthorized();
        $this->getJson('/api/estoque/boms')->assertUnauthorized();
    }

    public function test_recrutando_sem_acesso_ao_modulo(): void
    {
        $headers = $this->headersPapel(Role::RECRUTANDO);

        $this->getJson('/api/estoque/itens', $headers)->assertForbidden();
        $this->getJson('/api/estoque/emprestimos', $headers)->assertForbidden();
        $this->postJson('/api/estoque/itens', $this->itemPayload(), $headers)->assertForbidden();
    }

    public function test_estagiario_le_mas_nao_escreve(): void
    {
        $headers = $this->headersPapel(Role::ESTAGIARIO);

        $this->getJson('/api/estoque/itens', $headers)->assertOk();
        $this->getJson('/api/estoque/fornecedores', $headers)->assertOk();

        $item = $this->criarItem();
        $this->postJson('/api/estoque/itens', $this->itemPayload(), $headers)->assertForbidden();
        $this->postJson('/api/estoque/entradas', [
            'idItem' => $item->getKey(), 'idFornecedor' => 1,
            'quantidade' => '1.00', 'valorUnitario' => '1.00',
        ], $headers)->assertForbidden();
        $this->postJson('/api/estoque/emprestimos', [
            'idItem' => $item->getKey(), 'idPessoa' => 1,
            'quantidade' => '1.00',
            'dataDevolucaoPrevista' => now()->addDays(2)->toDateString(),
        ], $headers)->assertForbidden();
    }

    public function test_voluntario_sem_vinculo_nao_escreve_com_vinculo_escreve(): void
    {
        $sem = $this->headersPapel(Role::VOLUNTARIO);
        ['headers' => $com] = $this->responsavel(Role::VOLUNTARIO);

        $this->postJson('/api/estoque/itens', $this->itemPayload(), $sem)->assertForbidden();
        $this->postJson('/api/estoque/itens', $this->itemPayload(), $com)->assertCreated();
    }

    public function test_bolsista_sem_vinculo_nao_escreve(): void
    {
        $headers = $this->headersPapel(Role::BOLSISTA);

        $this->getJson('/api/estoque/itens', $headers)->assertOk();
        $this->postJson('/api/estoque/itens', $this->itemPayload(), $headers)->assertForbidden();
    }
}
