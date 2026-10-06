<?php

namespace Tests\Feature\Estoque;

use App\Modules\Auth\Enums\Role;
use App\Modules\Estoque\Models\Item;
use Illuminate\Http\UploadedFile;

class ItensTest extends EstoqueTestCase
{
    public function test_crud_item_com_localizacao_completa(): void
    {
        $headers = $this->headersAdmin();

        $locId = $this->postJson('/api/estoque/localizacoes', [
            'armario' => 'A1', 'prateleira' => 'P2', 'caixa' => 'C3', 'descricao' => 'Cantos',
        ], $headers)->assertCreated()->json('id');

        $id = $this->postJson('/api/estoque/itens', $this->itemPayload(['idLocalizacao' => $locId]), $headers)
            ->assertCreated()
            ->assertJsonPath('nome', 'Chapa de MDF')
            ->assertJsonPath('categoria', 'INSUMO')
            ->assertJsonPath('localizacao.armario', 'A1')
            ->json('id');

        $this->getJson("/api/estoque/itens/{$id}", $headers)
            ->assertOk()
            ->assertJsonPath('localizacao.caixa', 'C3');

        $this->putJson("/api/estoque/itens/{$id}", $this->itemPayload([
            'nome' => 'Chapa de MDF 18mm', 'quantidadeAtual' => '60.00',
        ]), $headers)->assertOk()->assertJsonPath('nome', 'Chapa de MDF 18mm');

        $this->assertSame('60.00', Item::find($id)->quantidade_atual);
    }

    public function test_listar_filtra_categoria_localizacao_baixo(): void
    {
        $headers = $this->headersAdmin();

        $locId = $this->postJson('/api/estoque/localizacoes', ['armario' => 'B1'], $headers)->json('id');
        $this->criarItem(['categoria' => 'INSUMO', 'quantidade_atual' => '5.00', 'estoque_minimo' => '10.00']);
        $this->criarItem(['categoria' => 'FERRAMENTA', 'localizacao_id' => $locId]);

        $this->getJson('/api/estoque/itens?categoria=INSUMO', $headers)->assertOk()->assertJsonCount(1);
        $this->getJson("/api/estoque/itens?idLocalizacao={$locId}", $headers)->assertOk()->assertJsonCount(1);
        $this->getJson('/api/estoque/itens?baixo=1', $headers)->assertOk()->assertJsonCount(1);
        $this->getJson('/api/estoque/itens', $headers)->assertOk()->assertJsonCount(2);
    }

    public function test_validacao_rejeita_antes_do_service(): void
    {
        $headers = $this->headersAdmin();

        $this->postJson('/api/estoque/itens', $this->itemPayload(['categoria' => 'LIXO']), $headers)
            ->assertStatus(422);
        $this->postJson('/api/estoque/itens', $this->itemPayload(['quantidadeAtual' => '-1']), $headers)
            ->assertStatus(422);
        $this->postJson('/api/estoque/itens', $this->itemPayload(['nome' => '']), $headers)
            ->assertStatus(422);
    }

    public function test_export_csv_sanitiza_injection(): void
    {
        $headers = $this->headersAdmin();

        $this->criarItem(['nome' => '=cmd|calc', 'descricao' => "+soma\tformula"]);

        $res = $this->get('/api/estoque/itens/export', $headers)->assertOk();
        $res->assertHeader('Content-Type', 'text/csv; charset=UTF-8');

        $corpo = $res->streamedContent();
        $this->assertStringStartsWith(
            "id_item;nome;descricao;categoria;unidade_medida;quantidade_atual;estoque_minimo;localizacao\n",
            $corpo,
        );
        $this->assertStringContainsString("'=cmd|calc", $corpo);
        $this->assertStringNotContainsString("\n=cmd|calc", $corpo);
        $this->assertStringContainsString("'+soma\tformula", $corpo);
    }

    public function test_import_csv_reaproveita_criar(): void
    {
        ['headers' => $headers] = $this->responsavel();

        $csv = "Parafuso;fixador;INSUMO;UN;10;2\nChave;ferramenta;FERRAMENTA;UN;3,5;1\n\n";
        $arquivo = UploadedFile::fake()->createWithContent('itens.csv', $csv);

        $this->withHeaders($headers)->post('/api/estoque/itens/import', ['arquivo' => $arquivo])
            ->assertCreated()
            ->assertJsonCount(2);

        $this->assertSame('3.50', Item::where('nome', 'Chave')->first()->quantidade_atual);

        $ruim = UploadedFile::fake()->createWithContent('ruim.csv', "só;duas;colunas\n");
        $this->withHeaders($headers)->post('/api/estoque/itens/import', ['arquivo' => $ruim])
            ->assertStatus(400);
    }

    public function test_sem_token_da_401(): void
    {
        $this->postJson('/api/estoque/itens', $this->itemPayload())->assertUnauthorized();
        $this->getJson('/api/estoque/itens')->assertUnauthorized();
    }

    public function test_sem_vinculo_nao_escreve(): void
    {
        $this->postJson('/api/estoque/itens', $this->itemPayload(), $this->headersPapel(Role::BOLSISTA))
            ->assertForbidden();
        $this->postJson('/api/estoque/itens', $this->itemPayload(), $this->headersPapel(Role::ESTAGIARIO))
            ->assertForbidden();
        $this->postJson('/api/estoque/itens', $this->itemPayload(), $this->headersPapel(Role::RECRUTANDO))
            ->assertForbidden();
    }
}
