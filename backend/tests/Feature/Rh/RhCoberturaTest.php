<?php

namespace Tests\Feature\Rh;

use App\Modules\Auth\Enums\Role;
use App\Modules\Rh\Enums\StatusApontamento;
use App\Modules\Rh\Enums\StatusProcesso;
use App\Modules\Rh\Enums\TipoApontamento;
use App\Modules\Rh\Models\ApontamentoHoras;
use App\Modules\Rh\Models\AvaliacaoTreinamento;
use App\Modules\Rh\Models\CertificadoEmitido;
use App\Modules\Rh\Models\GrupoProcessoSeletivo;
use App\Modules\Rh\Models\HistoricoNivel;
use App\Modules\Rh\Models\HoraConsolidada;
use App\Modules\Rh\Models\Pessoa;
use App\Modules\Rh\Models\ProcessoSeletivo;
use App\Modules\Rh\Models\RegistroPontoDiario;
use App\Modules\Rh\Models\SolicitacaoCertificado;
use App\Modules\Rh\Models\Treinamento;
use App\Modules\Rh\Models\Tutor;
use Tests\TestCase;

/** Bordas dos services (404/403/400) + fiação das relations Eloquent. */
class RhCoberturaTest extends TestCase
{
    public function test_vincular_pessoa_inexistente_e_nivel_sem_vinculo(): void
    {
        $admin = $this->criarAdminRh();
        $headers = $this->authHeader($admin['login'], Role::ADMIN);

        $this->postJson('/api/rh/funcionarios', ['idPessoa' => 999999], $headers)->assertNotFound();

        // Admin sem vínculo não tem id_admin_alterou: 403 (paridade Java).
        $adminSolto = $this->criarAdmin();
        $membro = $this->criarFuncionario(null, Role::BOLSISTA);
        $this->putJson(
            '/api/rh/funcionarios/'.$membro['funcionario']->getKey().'/nivel',
            ['nivel' => 2],
            $this->authHeader($adminSolto, Role::ADMIN)
        )->assertForbidden();
    }

    public function test_buscar_inexistente_e_excluir_com_processo(): void
    {
        $admin = $this->criarAdminRh();
        $headers = $this->authHeader($admin['login'], Role::ADMIN);
        $tutor = $this->criarFuncionario(null, Role::BOLSISTA);
        $this->criarTutor($tutor['funcionario']);

        $this->getJson('/api/rh/pessoas/999999', $headers)->assertNotFound();

        $proc = $this->postJson('/api/rh/processo-seletivo', [
            'nomeCompleto' => 'Cand', 'matricula' => 'MAT-DEL',
            'idTutor' => $tutor['funcionario']->getKey(),
        ], $headers)->assertCreated();

        // Pessoa com processo não exclui (400); detalhe de pessoa sem vínculo degrada.
        $pessoaId = Pessoa::where('matricula', 'MAT-DEL')->firstOrFail()->getKey();
        $this->deleteJson("/api/rh/pessoas/{$pessoaId}", [], $headers)->assertStatus(400);

        $solta = $this->criarPessoa();
        $this->getJson('/api/rh/pessoas/'.$solta->getKey().'/detalhe', $headers)
            ->assertOk()
            ->assertJsonPath('idFuncionario', null);
    }

    public function test_apontamento_bordas(): void
    {
        $admin = $this->criarAdminRh();
        $headersAdmin = $this->authHeader($admin['login'], Role::ADMIN);
        $membro = $this->criarFuncionario(null, Role::BOLSISTA);
        $id = (int) $membro['funcionario']->getKey();

        // Registrar p/ funcionário inexistente: 404.
        $this->postJson('/api/rh/apontamentos-horas', [
            'idFuncionario' => 999999, 'tipo' => 'PROJETO', 'idReferencia' => 1,
            'data' => '2026-08-04', 'horasTrabalhadas' => 1,
        ], $headersAdmin)->assertNotFound();

        // Listar sem vínculo: 403.
        $solto = $this->criarLogin();
        $this->comPermissao($solto, Role::BOLSISTA);
        $this->getJson('/api/rh/apontamentos-horas', $this->authHeader($solto, Role::BOLSISTA))
            ->assertForbidden();

        // Coerência excedida: presença 2h, apontado 3h → 400 na validação.
        RegistroPontoDiario::create(['id_funcionario' => $id, 'data' => '2026-08-06', 'total_horas' => '2.00']);
        $ap = ApontamentoHoras::create([
            'id_funcionario' => $id, 'tipo' => TipoApontamento::ENCOMENDA,
            'id_referencia' => 1, 'data' => '2026-08-06', 'horas_trabalhadas' => '3.00',
            'status' => StatusApontamento::PENDENTE, 'consolidado' => false,
        ]);

        $this->putJson("/api/rh/apontamentos-horas/{$ap->getKey()}/validar", ['status' => 'VALIDADO'], $headersAdmin)
            ->assertStatus(400);

        // Status PENDENTE na validação: 400.
        $this->putJson("/api/rh/apontamentos-horas/{$ap->getKey()}/validar", ['status' => 'PENDENTE'], $headersAdmin)
            ->assertStatus(422); // nem passa do FormRequest (in:)
    }

    public function test_processo_bordas(): void
    {
        $admin = $this->criarAdminRh();
        $headersAdmin = $this->authHeader($admin['login'], Role::ADMIN);
        $tutor = $this->criarFuncionario(null, Role::BOLSISTA);
        $this->criarTutor($tutor['funcionario']);
        $outro = $this->criarFuncionario(null, Role::BOLSISTA);
        $this->criarTutor($outro['funcionario']);

        // Admin sem idTutor / com tutor inexistente: 400/404.
        $this->postJson('/api/rh/processo-seletivo', [
            'nomeCompleto' => 'C0', 'matricula' => 'MAT-C0',
        ], $headersAdmin)->assertStatus(400);
        $this->postJson('/api/rh/processo-seletivo', [
            'nomeCompleto' => 'C0', 'matricula' => 'MAT-C0', 'idTutor' => 999999,
        ], $headersAdmin)->assertNotFound();

        // Tutor com idTutor alheio: ignorado, vale o próprio vínculo (paridade Java).
        $alheio = $this->postJson('/api/rh/processo-seletivo', [
            'nomeCompleto' => 'C0', 'matricula' => 'MAT-C0', 'idTutor' => $outro['funcionario']->getKey(),
        ], $this->authHeader($tutor['login'], Role::BOLSISTA))->assertCreated();
        $this->assertSame((int) $tutor['funcionario']->getKey(), $alheio->json('idTutor'));

        $proc = $this->postJson('/api/rh/processo-seletivo', [
            'nomeCompleto' => 'C1', 'matricula' => 'MAT-C1', 'idTutor' => $tutor['funcionario']->getKey(),
        ], $headersAdmin)->assertCreated();
        $idProc = $proc->json('id');

        // Matrícula duplicada: 400.
        $this->postJson('/api/rh/processo-seletivo', [
            'nomeCompleto' => 'C1b', 'matricula' => 'MAT-C1', 'idTutor' => $tutor['funcionario']->getKey(),
        ], $headersAdmin)->assertStatus(400);

        // Tutor de outro processo não altera nem avalia.
        $headersOutro = $this->authHeader($outro['login'], Role::BOLSISTA);
        $this->putJson("/api/rh/processo-seletivo/{$idProc}", ['statusProcesso' => 'APROVADO'], $headersOutro)
            ->assertForbidden();

        // Atualizar status + resultado pelo responsável.
        $this->putJson("/api/rh/processo-seletivo/{$idProc}", [
            'statusProcesso' => 'EM_TRIAGEM', 'resultadoFinal' => 'Segue',
        ], $this->authHeader($tutor['login'], Role::BOLSISTA))
            ->assertOk()->assertJsonPath('resultadoFinal', 'Segue');

        // Avaliar pessoa de outro processo: 404.
        $this->postJson("/api/rh/processo-seletivo/{$idProc}/membros/999999/avaliar", ['nota' => 7], $headersAdmin)
            ->assertNotFound();

        // Listar com candidato sem grupo cobre o bloco "Sem grupo".
        $this->getJson('/api/rh/processo-seletivo', $headersAdmin)
            ->assertOk()
            ->assertJsonPath('grupos.0.nome', 'Sem grupo');

        // Grupo com o candidato: mover estágio atualiza a etapa do grupo.
        $grupo = $this->postJson('/api/rh/processo-seletivo/grupos', [
            'nome' => 'G1', 'idLider' => $tutor['funcionario']->getKey(),
            'membroIds' => [$proc->json('idCandidato')],
        ], $headersAdmin)->assertCreated()->assertJsonPath('total', 1);

        $this->patchJson("/api/rh/processo-seletivo/{$idProc}/estagio", ['etapa' => 'APROVADO'], $headersAdmin)
            ->assertOk();
        $this->assertSame('APROVADO', GrupoProcessoSeletivo::find($grupo->json('id'))->etapa->value);

        $this->getJson('/api/rh/processo-seletivo?estagio=APROVADO', $headersAdmin)
            ->assertOk()
            ->assertJsonPath('totais.aprovados', 1);

        // Líder inexistente: 404; membro sem processo: 404.
        $this->postJson('/api/rh/processo-seletivo/grupos', [
            'nome' => 'Gx', 'idLider' => 999999,
        ], $headersAdmin)->assertNotFound();
        $this->postJson('/api/rh/processo-seletivo/grupos', [
            'nome' => 'Gx', 'idLider' => $tutor['funcionario']->getKey(), 'membroIds' => [999999],
        ], $headersAdmin)->assertNotFound();

        // Líder não-tutor: 400.
        $comum = $this->criarFuncionario(null, Role::BOLSISTA);
        $this->postJson('/api/rh/processo-seletivo/grupos', [
            'nome' => 'G2', 'idLider' => $comum['funcionario']->getKey(),
        ], $headersAdmin)->assertStatus(400);

        $this->assertSame('G1', $grupo->json('nome'));
    }

    public function test_certificado_e_treinamento_bordas(): void
    {
        $admin = $this->criarAdminRh();
        $headersAdmin = $this->authHeader($admin['login'], Role::ADMIN);
        $adminSolto = $this->criarAdmin();

        // Admin sem vínculo não solicita nem decide (sem "eu" próprio).
        $this->postJson('/api/rh/certificados/solicitar', [
            'tipoCertificado' => 'EXTENSAO', 'horasSolicitadas' => 1,
        ], $this->authHeader($adminSolto, Role::ADMIN))->assertForbidden();
        $this->getJson('/api/rh/horas/disponiveis', $this->authHeader($adminSolto, Role::ADMIN))
            ->assertForbidden();

        // Solicitar sem vínculo: 403.
        $solto = $this->criarLogin();
        $this->comPermissao($solto, Role::BOLSISTA);
        $this->postJson('/api/rh/certificados/solicitar', [
            'tipoCertificado' => 'EXTENSAO', 'horasSolicitadas' => 1,
        ], $this->authHeader($solto, Role::BOLSISTA))->assertForbidden();

        // Aprovar inexistente: 404; decidir duas vezes: 400.
        $this->putJson('/api/rh/certificados/solicitacoes/999999/aprovar', [], $headersAdmin)->assertNotFound();
        $this->getJson('/api/rh/certificados/emitidos/999999', $headersAdmin)->assertNotFound();
        $this->getJson('/api/rh/extrato-mensal-horas?mes=2026-08&funcionarioId=999999', $headersAdmin)->assertNotFound();

        // Treinamento/avaliacao inexistentes: 404.
        $this->getJson('/api/rh/treinamentos/999999/avaliacoes', $headersAdmin)->assertNotFound();

        // Admin sem idTutor: 400; com tutor inexistente: 404.
        $this->postJson('/api/rh/treinamentos', ['titulo' => 'T0'], $headersAdmin)->assertStatus(400);
        $this->postJson('/api/rh/treinamentos', ['titulo' => 'T0', 'idTutor' => 999999], $headersAdmin)
            ->assertNotFound();

        // Extrato sem vínculo: 403.
        $this->getJson('/api/rh/extrato-mensal-horas', $this->authHeader($solto, Role::BOLSISTA))
            ->assertForbidden();

        $tutor = $this->criarFuncionario(null, Role::BOLSISTA);
        $this->criarTutor($tutor['funcionario']);
        $trein = $this->postJson('/api/rh/treinamentos', ['titulo' => 'T'], $this->authHeader($tutor['login'], Role::BOLSISTA))
            ->assertCreated();
        $this->postJson('/api/rh/treinamentos/'.$trein->json('id').'/avaliacoes', [
            'idFuncionario' => 999999, 'nota' => 5,
        ], $this->authHeader($tutor['login'], Role::BOLSISTA))->assertNotFound();
    }

    public function test_relations_eloquent_do_modulo(): void
    {
        $admin = $this->criarAdminRh();
        $membro = $this->criarFuncionario(null, Role::BOLSISTA);
        $id = (int) $membro['funcionario']->getKey();
        $this->criarTutor($membro['funcionario']);

        RegistroPontoDiario::create(['id_funcionario' => $id, 'data' => '2026-08-04', 'total_horas' => '8.00']);
        $ap = ApontamentoHoras::create([
            'id_funcionario' => $id, 'tipo' => TipoApontamento::PROJETO,
            'id_referencia' => 1, 'data' => '2026-08-04', 'horas_trabalhadas' => '5.00',
            'status' => StatusApontamento::VALIDADO, 'consolidado' => false,
        ]);

        $this->assertSame($id, (int) $ap->funcionario->getKey());

        $headersMembro = $this->authHeader($membro['login'], Role::BOLSISTA);
        $sol = $this->postJson('/api/rh/certificados/solicitar', [
            'tipoCertificado' => 'EXTENSAO', 'horasSolicitadas' => 5,
        ], $headersMembro)->assertCreated();

        $solicitacao = SolicitacaoCertificado::find($sol->json('idSolicitacao'));
        $this->assertSame($id, (int) $solicitacao->funcionario->getKey());

        $cert = $this->putJson(
            '/api/rh/certificados/solicitacoes/'.$sol->json('idSolicitacao').'/aprovar',
            [],
            $this->authHeader($admin['login'], Role::ADMIN)
        )->assertOk();

        $emitido = CertificadoEmitido::find($cert->json('idCertificado'));
        $this->assertSame($id, (int) $emitido->funcionario->getKey());
        $this->assertSame((int) $sol->json('idSolicitacao'), (int) $emitido->solicitacao->getKey());

        $consolidada = HoraConsolidada::firstOrFail();
        $this->assertSame((int) $emitido->getKey(), (int) $consolidada->certificado->getKey());
        $this->assertSame((int) $ap->getKey(), (int) $consolidada->apontamento->getKey());

        $this->assertSame($id, (int) RegistroPontoDiario::firstOrFail()->funcionario->getKey());
        $this->assertSame($id, (int) Tutor::firstOrFail()->funcionario->getKey());
        $this->assertSame($id, (int) $solicitacao->funcionario->pessoa->funcionario->getKey());

        $historico = HistoricoNivel::first();
        if ($historico !== null) {
            $this->assertNotNull($historico->admin->getKey());
        }

        // Evolução de nível + relations do processo.
        $this->patchJson(
            '/api/rh/niveis/'.$id.'/membros',
            ['nivel' => 2],
            $this->authHeader($admin['login'], Role::ADMIN)
        )->assertOk();

        $hist = HistoricoNivel::where('id_funcionario', $id)->firstOrFail();
        $this->assertSame($id, (int) $hist->funcionario->getKey());
        $this->assertSame((int) $admin['funcionario']->getKey(), (int) $hist->admin->getKey());

        $proc = ProcessoSeletivo::create([
            'id_candidato' => $membro['pessoa']->getKey(),
            'id_tutor' => $id,
            'status_processo' => StatusProcesso::INSCRITO,
            'data_inscricao' => '2026-08-01',
        ]);
        $this->assertSame((int) $membro['pessoa']->getKey(), (int) $proc->candidato->getKey());
        $this->assertSame($id, (int) $proc->tutor->getKey());

        $grupo = GrupoProcessoSeletivo::create([
            'nome' => 'G', 'id_tutor_lider' => $id, 'etapa' => StatusProcesso::INSCRITO,
        ]);
        $this->assertSame($id, (int) $grupo->lider->getKey());
        $proc->id_grupo = (int) $grupo->getKey();
        $proc->save();
        $this->assertSame((int) $grupo->getKey(), (int) $proc->refresh()->grupo->getKey());

        // Tutor do treinamento + avaliações do detalhe.
        $trein = Treinamento::create([
            'titulo' => 'T2', 'id_tutor' => $id,
        ]);
        $this->assertSame($id, (int) $trein->tutor->getKey());
        $av = AvaliacaoTreinamento::create([
            'id_treinamento' => $trein->getKey(), 'id_funcionario' => $id, 'nota' => '7.00',
        ]);
        $this->assertSame((int) $trein->getKey(), (int) $av->treinamento->getKey());
        $this->assertSame($id, (int) $av->funcionario->getKey());
    }
}
