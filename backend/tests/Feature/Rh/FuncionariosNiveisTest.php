<?php

namespace Tests\Feature\Rh;

use App\Enums\Auth\Role;
use App\Enums\Rh\StatusApontamento;
use App\Enums\Rh\TipoApontamento;
use App\Events\NivelAlteradoEvent;
use App\Models\Auth\UserPermission;
use App\Models\Rh\ApontamentoHoras;
use App\Models\Rh\Funcionario;
use App\Models\Rh\HistoricoNivel;
use App\Models\Rh\RegistroPontoDiario;
use Illuminate\Support\Facades\Event;
use Tests\TestCase;

class FuncionariosNiveisTest extends TestCase
{
    public function test_vincular_listar_e_matriz_sao_admin(): void
    {
        $admin = $this->criarAdminRh();
        $headers = $this->authHeader($admin['login'], Role::ADMIN);

        $pessoa = $this->criarPessoa();

        $this->postJson('/api/rh/funcionarios', [
            'idPessoa' => $pessoa->getKey(), 'nivelAcesso' => 2, 'departamento' => 'MECANICA',
        ], $headers)
            ->assertCreated()
            ->assertJsonPath('nivelAcesso', 'VOLUNTARIO');

        // Vínculo duplicado dá 422.
        $this->postJson('/api/rh/funcionarios', ['idPessoa' => $pessoa->getKey()], $headers)
            ->assertStatus(422);

        $this->getJson('/api/rh/funcionarios?nivel=VOLUNTARIO', $headers)
            ->assertOk()
            ->assertJsonCount(1);

        $this->getJson('/api/rh/niveis', $headers)
            ->assertOk()
            ->assertJsonStructure(['niveis', 'permissoes']);
    }

    public function test_alterar_nivel_grava_historico_e_emite_evento(): void
    {
        Event::fake([NivelAlteradoEvent::class]);

        $admin = $this->criarAdminRh();
        $membro = $this->criarFuncionario(null, Role::BOLSISTA);

        $res = $this->putJson(
            '/api/rh/funcionarios/'.$membro['funcionario']->getKey().'/nivel',
            ['nivel' => 2],
            $this->authHeader($admin['login'], Role::ADMIN)
        );

        $res->assertOk()
            ->assertJsonPath('nivelAntigo', 1)
            ->assertJsonPath('nivelNovo', 2);

        $this->assertSame(2, Funcionario::find($membro['funcionario']->getKey())->nivel_acesso->value);
        $this->assertSame(1, HistoricoNivel::where('id_funcionario', $membro['funcionario']->getKey())->count());

        // Espelho no RBAC do Auth (id_pessoa == id_user): papel ativo acompanha o nível.
        $this->assertSame(
            Role::VOLUNTARIO,
            UserPermission::where('id_user', $membro['pessoa']->getKey())->where('active', true)->first()?->role,
        );

        Event::assertDispatched(
            NivelAlteradoEvent::class,
            fn (NivelAlteradoEvent $e) => $e->idFuncionario === (int) $membro['funcionario']->getKey()
                && $e->nivelNovo->value === 2
        );
    }

    public function test_alterar_nivel_sem_mudanca_nao_grava_nem_emite(): void
    {
        Event::fake([NivelAlteradoEvent::class]);

        $admin = $this->criarAdminRh();
        $membro = $this->criarFuncionario(null, Role::BOLSISTA);

        $this->putJson(
            '/api/rh/funcionarios/'.$membro['funcionario']->getKey().'/nivel',
            ['nivel' => 1],
            $this->authHeader($admin['login'], Role::ADMIN)
        )->assertOk();

        $this->assertSame(0, HistoricoNivel::count());
        Event::assertNotDispatched(NivelAlteradoEvent::class);
    }

    public function test_convite_cria_pessoa_e_funcionario_recrutando(): void
    {
        $admin = $this->criarAdminRh();

        $this->postJson('/api/rh/niveis/convites', [
            'nomeCompleto' => 'Candidato X', 'matricula' => 'MAT-CX', 'nivelAcesso' => 4,
        ], $this->authHeader($admin['login'], Role::ADMIN))
            ->assertCreated()
            ->assertJsonPath('nivelAcesso', 'RECRUTANDO');

        $this->patchJson(
            '/api/rh/niveis/'.$admin['funcionario']->getKey().'/membros',
            ['nivel' => 1],
            $this->authHeader($admin['login'], Role::ADMIN)
        )->assertOk()->assertJsonPath('nivelNovo', 1);
    }

    public function test_total_horas_proprio_e_incoerencia(): void
    {
        $membro = $this->criarFuncionario(null, Role::BOLSISTA);
        $headers = $this->authHeader($membro['login'], Role::BOLSISTA);
        $id = (int) $membro['funcionario']->getKey();

        $res = $this->getJson("/api/rh/funcionarios/{$id}/horas", $headers);

        $res->assertOk()->assertJsonStructure([
            'idFuncionario', 'totalHorasPresenca', 'totalHorasEncomenda',
            'totalHorasProjeto', 'incoerencias',
        ]);

        // Dia incoerente (apontado > presença) aparece na lista p/ o Financeiro.
        RegistroPontoDiario::create([
            'id_funcionario' => $id, 'data' => '2026-08-04', 'total_horas' => '2.00',
        ]);
        ApontamentoHoras::create([
            'id_funcionario' => $id, 'tipo' => TipoApontamento::ENCOMENDA,
            'id_referencia' => 1, 'data' => '2026-08-04', 'horas_trabalhadas' => '3.00',
            'status' => StatusApontamento::PENDENTE, 'consolidado' => false,
        ]);

        $this->getJson("/api/rh/funcionarios/{$id}/horas", $headers)
            ->assertOk()
            ->assertJsonPath('incoerencias.0.data', '2026-08-04')
            ->assertJsonPath('incoerencias.0.horasPresenca', '2.00')
            ->assertJsonPath('incoerencias.0.horasApontadas', '3.00');

        // Terceiro não enxerga.
        $outro = $this->criarFuncionario(null, Role::BOLSISTA);
        $this->getJson(
            "/api/rh/funcionarios/{$id}/horas",
            $this->authHeader($outro['login'], Role::BOLSISTA)
        )->assertForbidden();
    }

    public function test_vincular_preserva_admin_quando_login_ja_e_admin(): void
    {
        $adminRh = $this->criarAdminRh();
        $headers = $this->authHeader($adminRh['login'], Role::ADMIN);

        $loginAdmin = $this->criarLogin();
        $this->comPermissao($loginAdmin, Role::ADMIN);
        $pessoa = $this->criarPessoa(['id' => $loginAdmin->id_user]);

        $this->postJson('/api/rh/funcionarios', [
            'idPessoa' => $pessoa->getKey(),
            'nivelAcesso' => Role::BOLSISTA->value,
            'departamento' => 'ELETRONICA',
        ], $headers)
            ->assertCreated()
            ->assertJsonPath('nivelAcesso', 'ADMIN');

        $this->assertDatabaseHas('rh.funcionario', [
            'id_pessoa' => (int) $pessoa->getKey(),
            'nivel_acesso' => Role::ADMIN->value,
        ]);
        $this->assertDatabaseHas('auth.user_permissions', [
            'id_user' => (int) $loginAdmin->id_user,
            'role' => Role::ADMIN->value,
            'active' => true,
        ]);
    }

    public function test_vincular_sem_login_nao_cria_user_permission_orfa(): void
    {
        $adminRh = $this->criarAdminRh();
        $headers = $this->authHeader($adminRh['login'], Role::ADMIN);

        $pessoa = $this->criarPessoa();

        $this->postJson('/api/rh/funcionarios', [
            'idPessoa' => $pessoa->getKey(),
            'nivelAcesso' => Role::VOLUNTARIO->value,
            'departamento' => 'MECANICA',
        ], $headers)
            ->assertCreated()
            ->assertJsonPath('nivelAcesso', 'VOLUNTARIO');

        $this->assertDatabaseHas('rh.funcionario', [
            'id_pessoa' => (int) $pessoa->getKey(),
            'nivel_acesso' => Role::VOLUNTARIO->value,
        ]);
        $this->assertDatabaseMissing('auth.user_permissions', [
            'id_user' => (int) $pessoa->getKey(),
        ]);
    }

    public function test_matriz_de_niveis_expoe_membros_com_campos(): void
    {
        $adminRh = $this->criarAdminRh();
        $membro = $this->criarFuncionario(null, Role::BOLSISTA);
        $headers = $this->authHeader($adminRh['login'], Role::ADMIN);

        $res = $this->getJson('/api/rh/niveis', $headers)
            ->assertOk()
            ->assertJsonStructure(['niveis', 'membros', 'permissoes'])
            ->assertJsonStructure(['membros' => [['id', 'name', 'matricula', 'nivel', 'grupo']]]);

        $found = collect($res->json('membros'))->firstWhere('id', (int) $membro['funcionario']->getKey());

        $this->assertNotNull($found);
        $this->assertSame($membro['pessoa']->nome_completo, $found['name']);
        $this->assertSame($membro['pessoa']->matricula, $found['matricula']);
        $this->assertSame('bolsista', $found['nivel']);
        $this->assertSame($membro['funcionario']->departamento, $found['grupo']);
    }
}
