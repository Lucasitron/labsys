<?php

namespace Tests\Feature\Rh;

use App\Modules\Auth\Enums\Role;
use App\Modules\Rh\Enums\NivelAcesso;
use App\Modules\Rh\Enums\PessoaStatus;
use App\Modules\Rh\Models\Pessoa;
use Tests\TestCase;

class ProcessoTreinamentoTest extends TestCase
{
    public function test_processo_cria_candidato_recrutando_e_evolui(): void
    {
        $admin = $this->criarAdminRh();
        $tutor = $this->criarFuncionario(null, Role::BOLSISTA);
        $this->criarTutor($tutor['funcionario']);
        $headersAdmin = $this->authHeader($admin['login'], Role::ADMIN);
        $headersTutor = $this->authHeader($tutor['login'], Role::BOLSISTA);

        $proc = $this->postJson('/api/rh/processo-seletivo', [
            'nomeCompleto' => 'Candidato Y', 'matricula' => 'MAT-CY',
            'idTutor' => $tutor['funcionario']->getKey(),
        ], $headersAdmin)->assertCreated()->assertJsonPath('statusProcesso', 'INSCRITO');

        $pessoa = Pessoa::where('matricula', 'MAT-CY')->firstOrFail();
        $this->assertSame(PessoaStatus::RECRUTANDO, $pessoa->status);
        $this->assertSame(NivelAcesso::RECRUTANDO, $pessoa->funcionario->nivel_acesso);

        // Tutor de outro processo não mexe; o responsável move e avalia.
        $this->patchJson(
            '/api/rh/processo-seletivo/'.$proc->json('id').'/estagio',
            ['etapa' => 'ENTREVISTA'],
            $headersTutor
        )->assertOk()->assertJsonPath('statusProcesso', 'ENTREVISTA');

        $this->postJson(
            '/api/rh/processo-seletivo/'.$proc->json('id').'/membros/'.$pessoa->getKey().'/avaliar',
            ['nota' => 8.5, 'feedback' => 'Bom'],
            $headersTutor
        )->assertStatus(201)->assertJsonPath('nota', '8.50');

        // Nota fora de 0-10: 422.
        $this->postJson(
            '/api/rh/processo-seletivo/'.$proc->json('id').'/membros/'.$pessoa->getKey().'/avaliar',
            ['nota' => 11],
            $headersTutor
        )->assertStatus(422);

        // Kanban por grupos + totais.
        $grupo = $this->postJson('/api/rh/processo-seletivo/grupos', [
            'nome' => 'Eletrônica', 'idLider' => $tutor['funcionario']->getKey(),
            'membroIds' => [$pessoa->getKey()],
        ], $headersTutor)->assertCreated();

        $this->assertSame(1, $grupo->json('total'));

        $this->getJson('/api/rh/processo-seletivo', $headersTutor)
            ->assertOk()
            ->assertJsonStructure(['grupos', 'totais']);
    }

    public function test_bolsista_nao_tutor_nao_inicia_processo(): void
    {
        $membro = $this->criarFuncionario(null, Role::BOLSISTA);

        $this->postJson('/api/rh/processo-seletivo', [
            'nomeCompleto' => 'Z', 'matricula' => 'MAT-ZZ',
        ], $this->authHeader($membro['login'], Role::BOLSISTA))->assertForbidden();
    }

    public function test_treinamento_e_avaliacao_por_tutor(): void
    {
        $admin = $this->criarAdminRh();
        $tutor = $this->criarFuncionario(null, Role::BOLSISTA);
        $this->criarTutor($tutor['funcionario']);
        $aluno = $this->criarFuncionario(null, Role::VOLUNTARIO);
        $headersTutor = $this->authHeader($tutor['login'], Role::BOLSISTA);

        $trein = $this->postJson('/api/rh/treinamentos', [
            'titulo' => 'Solda Básica', 'descricao' => 'Guia', 'urlConteudo' => 'http://x',
        ], $headersTutor)->assertCreated()->assertJsonPath('titulo', 'Solda Básica');

        // Admin cria informando o tutor.
        $this->postJson('/api/rh/treinamentos', [
            'titulo' => 'Admin cria', 'idTutor' => $tutor['funcionario']->getKey(),
        ], $this->authHeader($admin['login'], Role::ADMIN))->assertCreated();

        $this->postJson('/api/rh/treinamentos/'.$trein->json('id').'/avaliacoes', [
            'idFuncionario' => $aluno['funcionario']->getKey(), 'nota' => 9, 'feedback' => 'Ótimo',
        ], $headersTutor)->assertStatus(201);

        $this->getJson('/api/rh/treinamentos/'.$trein->json('id').'/avaliacoes', $headersTutor)
            ->assertOk()->assertJsonCount(1);

        // Bolsista não-tutor não avalia.
        $comum = $this->criarFuncionario(null, Role::BOLSISTA);
        $this->postJson('/api/rh/treinamentos/'.$trein->json('id').'/avaliacoes', [
            'idFuncionario' => $aluno['funcionario']->getKey(), 'nota' => 5,
        ], $this->authHeader($comum['login'], Role::BOLSISTA))->assertForbidden();
    }
}
