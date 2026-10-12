<?php

namespace Tests\Feature\Rh;

use App\Modules\Auth\Enums\Role;
use App\Modules\Auth\Models\Login;
use App\Modules\Rh\Enums\PessoaStatus;
use App\Modules\Rh\Models\Pessoa;
use Tests\TestCase;

class PessoasTest extends TestCase
{
    public function test_cadastrar_exige_admin_ou_membro_e_mascara_cpf(): void
    {
        $admin = $this->criarAdminRh();

        $res = $this->postJson('/api/rh/pessoas', [
            'nomeCompleto' => 'Ada Lovelace',
            'matricula' => 'MAT-ADA',
            'contato' => 'ada@fablab.org',
            'turno' => 'MANHA',
            'status' => 0,
            'cpf' => '529.982.247-25',
        ], $this->authHeader($admin['login'], Role::ADMIN));

        $res->assertCreated()
            ->assertJsonPath('cpf', '***.***.***-25')
            ->assertJsonPath('status', 'ATIVO');

        // Persistido só com dígitos.
        $this->assertSame('52998224725', Pessoa::where('matricula', 'MAT-ADA')->firstOrFail()->cpf);
    }

    public function test_matricula_duplicada_cpf_invalido_e_cpf_duplicado_dao_422(): void
    {
        $admin = $this->criarAdminRh();
        $headers = $this->authHeader($admin['login'], Role::ADMIN);

        $base = ['nomeCompleto' => 'A', 'matricula' => 'MAT-X', 'cpf' => '529.982.247-25'];

        $this->postJson('/api/rh/pessoas', $base, $headers)->assertCreated();
        $this->postJson('/api/rh/pessoas', $base, $headers)->assertStatus(422);
        // Formato/dígitos inválidos barram no FormRequest (422, padrão M1); duplicado no service (422).
        $this->postJson('/api/rh/pessoas', ['nomeCompleto' => 'B', 'matricula' => 'MAT-Y', 'cpf' => '111.111.111-11'], $headers)
            ->assertStatus(422);
        $this->postJson('/api/rh/pessoas', ['nomeCompleto' => 'C', 'matricula' => 'MAT-Z', 'cpf' => '52998224725'], $headers)
            ->assertStatus(422);
    }

    public function test_listar_com_busca_paginacao_e_facetas(): void
    {
        $admin = $this->criarAdminRh();
        $headers = $this->authHeader($admin['login'], Role::ADMIN);

        $res = $this->getJson('/api/rh/pessoas?search=ELETRONICA&page=1&pageSize=10', $headers);

        $res->assertOk()
            ->assertJsonStructure(['pessoas', 'paginacao', 'filtros'])
            ->assertJsonPath('paginacao.page', 1);

        // CPF nunca sai em claro.
        foreach ($res->json('pessoas') as $pessoa) {
            $this->assertTrue($pessoa['cpf'] === null || str_starts_with($pessoa['cpf'], '***'));
        }
    }

    public function test_estagiario_ve_so_o_proprio_e_recrutando_nao_acessa(): void
    {
        $estagiario = $this->criarFuncionario(null, Role::ESTAGIARIO);
        $outro = $this->criarFuncionario(null, Role::BOLSISTA);

        $headers = $this->authHeader($estagiario['login'], Role::ESTAGIARIO);

        $lista = $this->getJson('/api/rh/pessoas', $headers)->assertOk();
        $this->assertCount(1, $lista->json('pessoas'));
        $this->assertSame((int) $estagiario['pessoa']->getKey(), $lista->json('pessoas.0.id'));

        $this->getJson('/api/rh/pessoas/'.$outro['pessoa']->getKey(), $headers)->assertForbidden();
        $this->getJson('/api/rh/pessoas/'.$estagiario['pessoa']->getKey(), $headers)->assertOk();

        $recrutando = $this->criarFuncionario(null, Role::RECRUTANDO);
        $this->getJson('/api/rh/pessoas', $this->authHeader($recrutando['login'], Role::RECRUTANDO))
            ->assertForbidden();
    }

    public function test_detalhe_agregado_e_exclusao_com_vinculo_bloqueada(): void
    {
        $admin = $this->criarAdminRh();
        $headers = $this->authHeader($admin['login'], Role::ADMIN);
        $membro = $this->criarFuncionario(null, Role::BOLSISTA);

        $this->getJson('/api/rh/pessoas/'.$membro['pessoa']->getKey().'/detalhe', $headers)
            ->assertOk()
            ->assertJsonStructure([
                'pessoa', 'idFuncionario', 'nivel', 'horasMes', 'treinamentos',
                'pendencias', 'horasPorStatus', 'avaliacoes', 'historicoNivel',
            ]);

        // Com vínculo: excluir dá 422.
        $this->deleteJson('/api/rh/pessoas/'.$membro['pessoa']->getKey(), [], $headers)->assertStatus(422);

        // Sem vínculo: exclui.
        $solta = $this->criarPessoa();
        $this->deleteJson('/api/rh/pessoas/'.$solta->getKey(), [], $headers)
            ->assertOk()
            ->assertJson(['excluido' => true]);
    }

    public function test_cadastro_com_vinculo_e_acesso_provisiona_login_papel_e_funcionario(): void
    {
        $admin = $this->criarAdminRh();
        $headers = $this->authHeader($admin['login'], Role::ADMIN);

        $res = $this->postJson('/api/rh/pessoas', [
            'nomeCompleto' => 'Grace Hopper',
            'matricula' => 'MAT-GRACE',
            'nivelAcesso' => Role::BOLSISTA->value,
            'departamento' => 'ELETRONICA',
            'criarAcesso' => true,
            'email' => 'grace@fablab.org',
            'senha' => 'segredo123',
        ], $headers)->assertCreated();

        $idPessoa = (int) $res->json('id');

        $this->assertDatabaseHas('rh.funcionario', [
            'id_pessoa' => $idPessoa,
            'nivel_acesso' => Role::BOLSISTA->value,
            'departamento' => 'ELETRONICA',
        ]);
        $this->assertDatabaseHas('auth.login', [
            'id_user' => $idPessoa,
            'email' => 'grace@fablab.org',
            'nome_usuario' => 'grace',
            'situacao' => 'ATIVO',
        ]);
        $this->assertDatabaseHas('auth.user_permissions', [
            'id_user' => $idPessoa,
            'role' => Role::BOLSISTA->value,
            'active' => true,
        ]);
    }

    public function test_cadastro_com_convite_cria_acesso_pendente(): void
    {
        $admin = $this->criarAdminRh();
        $headers = $this->authHeader($admin['login'], Role::ADMIN);

        $res = $this->postJson('/api/rh/pessoas', [
            'nomeCompleto' => 'Alan Turing',
            'matricula' => 'MAT-ALAN',
            'nivelAcesso' => Role::ESTAGIARIO->value,
            'criarAcesso' => true,
            'email' => 'alan@fablab.org',
        ], $headers)->assertCreated();

        $this->assertDatabaseHas('auth.login', [
            'id_user' => (int) $res->json('id'),
            'situacao' => 'PENDENTE',
            'nome_usuario' => 'alan',
        ]);
    }

    public function test_criar_acesso_exige_email_e_falha_reverte_a_pessoa(): void
    {
        $admin = $this->criarAdminRh();
        $headers = $this->authHeader($admin['login'], Role::ADMIN);

        // Sem e-mail quando criarAcesso=true → 422 (FormRequest).
        $this->postJson('/api/rh/pessoas', [
            'nomeCompleto' => 'Sem Email', 'matricula' => 'MAT-SEM-EMAIL', 'criarAcesso' => true,
        ], $headers)->assertStatus(422);

        // E-mail já em uso: o provisionamento falha e a transação reverte tudo.
        $this->postJson('/api/rh/pessoas', [
            'nomeCompleto' => 'Original', 'matricula' => 'MAT-ORIG',
            'criarAcesso' => true, 'email' => 'dup@fablab.org',
        ], $headers)->assertCreated();

        $this->postJson('/api/rh/pessoas', [
            'nomeCompleto' => 'Duplicado', 'matricula' => 'MAT-DUP2',
            'criarAcesso' => true, 'email' => 'dup@fablab.org',
        ], $headers)->assertStatus(422);

        $this->assertDatabaseMissing('rh.pessoa', ['matricula' => 'MAT-DUP2']);
        $this->assertSame(1, Login::where('email', 'dup@fablab.org')->count());
    }

    public function test_atualizar_status_para_inativo(): void
    {
        $admin = $this->criarAdminRh();
        $headers = $this->authHeader($admin['login'], Role::ADMIN);
        $membro = $this->criarFuncionario(null, Role::BOLSISTA);

        $this->putJson('/api/rh/pessoas/'.$membro['pessoa']->getKey(), [
            'nomeCompleto' => $membro['pessoa']->nome_completo,
            'matricula' => $membro['pessoa']->matricula,
            'status' => PessoaStatus::INATIVO->value,
        ], $headers)
            ->assertOk()
            ->assertJsonPath('status', 'INATIVO');
    }
}
