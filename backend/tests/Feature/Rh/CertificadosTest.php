<?php

namespace Tests\Feature\Rh;

use App\Modules\Auth\Enums\Role;
use App\Modules\Rh\Enums\StatusApontamento;
use App\Modules\Rh\Enums\TipoApontamento;
use App\Modules\Rh\Events\CertificadoAprovadoEvent;
use App\Modules\Rh\Events\CertificadoRejeitadoEvent;
use App\Modules\Rh\Events\CertificadoSolicitadoEvent;
use App\Modules\Rh\Models\ApontamentoHoras;
use App\Modules\Rh\Models\CertificadoEmitido;
use App\Modules\Rh\Models\HoraConsolidada;
use App\Modules\Rh\Models\RegistroPontoDiario;
use Illuminate\Support\Facades\Event;
use Tests\TestCase;

class CertificadosTest extends TestCase
{
    private function comHorasValidadas(int $idFuncionario, string $horas = '5.00'): void
    {
        RegistroPontoDiario::create([
            'id_funcionario' => $idFuncionario, 'data' => '2026-08-04', 'total_horas' => '8.00',
        ]);
        ApontamentoHoras::create([
            'id_funcionario' => $idFuncionario, 'tipo' => TipoApontamento::ENCOMENDA,
            'id_referencia' => 3, 'data' => '2026-08-04', 'horas_trabalhadas' => $horas,
            'status' => StatusApontamento::VALIDADO, 'consolidado' => false,
        ]);
    }

    public function test_solicitar_aprovar_consolida_tudo_com_uuid(): void
    {
        Event::fake([CertificadoSolicitadoEvent::class, CertificadoAprovadoEvent::class]);

        $admin = $this->criarAdminRh();
        $membro = $this->criarFuncionario(null, Role::BOLSISTA);
        $id = (int) $membro['funcionario']->getKey();
        $this->comHorasValidadas($id);

        $headersMembro = $this->authHeader($membro['login'], Role::BOLSISTA);

        $this->getJson('/api/rh/horas/disponiveis', $headersMembro)
            ->assertOk()
            ->assertJsonPath('totalHorasDisponiveis', '5.00');

        // Acima do disponível: 400.
        $this->postJson('/api/rh/certificados/solicitar', [
            'tipoCertificado' => 'EXTENSAO', 'horasSolicitadas' => 99,
        ], $headersMembro)->assertStatus(400);

        $sol = $this->postJson('/api/rh/certificados/solicitar', [
            'tipoCertificado' => 'EXTENSAO', 'horasSolicitadas' => 5,
        ], $headersMembro)->assertCreated()->assertJsonPath('status', 'PENDENTE');

        Event::assertDispatched(CertificadoSolicitadoEvent::class, 1);

        $headersAdmin = $this->authHeader($admin['login'], Role::ADMIN);
        $aprov = $this->putJson(
            '/api/rh/certificados/solicitacoes/'.$sol->json('idSolicitacao').'/aprovar',
            ['observacao' => 'OK'],
            $headersAdmin
        )->assertOk()->assertJsonPath('horasCertificadas', '5.00');

        $this->assertNotEmpty($aprov->json('codigoVerificacao'));

        // Consolidação total: nada volta a disponíveis.
        $this->assertSame(1, CertificadoEmitido::count());
        $this->assertSame(1, HoraConsolidada::count());
        $this->assertSame(
            0,
            ApontamentoHoras::where('id_funcionario', $id)->where('consolidado', false)->count()
        );

        $this->getJson('/api/rh/horas/disponiveis', $headersMembro)
            ->assertOk()
            ->assertJsonPath('totalHorasDisponiveis', '0.00');

        Event::assertDispatched(CertificadoAprovadoEvent::class, 1);

        // Emitidos: dono vê o seu; terceiros não.
        $idCert = $aprov->json('idCertificado');
        $this->getJson('/api/rh/certificados/emitidos', $headersMembro)->assertOk()->assertJsonCount(1);
        $this->getJson("/api/rh/certificados/emitidos/{$idCert}", $headersMembro)->assertOk();

        $outro = $this->criarFuncionario(null, Role::BOLSISTA);
        $this->getJson(
            "/api/rh/certificados/emitidos/{$idCert}",
            $this->authHeader($outro['login'], Role::BOLSISTA)
        )->assertForbidden();
    }

    public function test_rejeitar_mantem_disponiveis_e_decidir_e_admin(): void
    {
        Event::fake([CertificadoRejeitadoEvent::class]);

        $admin = $this->criarAdminRh();
        $membro = $this->criarFuncionario(null, Role::BOLSISTA);
        $id = (int) $membro['funcionario']->getKey();
        $this->comHorasValidadas($id);

        $headersMembro = $this->authHeader($membro['login'], Role::BOLSISTA);

        $sol = $this->postJson('/api/rh/certificados/solicitar', [
            'tipoCertificado' => 'ESTAGIO', 'horasSolicitadas' => 5,
        ], $headersMembro)->assertCreated();

        // Não-admin não decide.
        $this->putJson(
            '/api/rh/certificados/solicitacoes/'.$sol->json('idSolicitacao').'/aprovar',
            [],
            $headersMembro
        )->assertForbidden();

        $this->putJson(
            '/api/rh/certificados/solicitacoes/'.$sol->json('idSolicitacao').'/rejeitar',
            ['observacao' => 'Sem carga suficiente'],
            $this->authHeader($admin['login'], Role::ADMIN)
        )->assertOk()->assertJsonPath('status', 'REJEITADO');

        $this->assertSame(0, CertificadoEmitido::count());
        $this->getJson('/api/rh/horas/disponiveis', $headersMembro)
            ->assertOk()
            ->assertJsonPath('totalHorasDisponiveis', '5.00');

        Event::assertDispatched(CertificadoRejeitadoEvent::class, 1);
    }

    public function test_decidir_duas_vezes_e_corrida_por_horas(): void
    {
        $admin = $this->criarAdminRh();
        $membro = $this->criarFuncionario(null, Role::BOLSISTA);
        $id = (int) $membro['funcionario']->getKey();
        $this->comHorasValidadas($id);
        $headersMembro = $this->authHeader($membro['login'], Role::BOLSISTA);
        $headersAdmin = $this->authHeader($admin['login'], Role::ADMIN);

        $a = $this->postJson('/api/rh/certificados/solicitar', [
            'tipoCertificado' => 'EXTENSAO', 'horasSolicitadas' => 5,
        ], $headersMembro)->assertCreated();
        $b = $this->postJson('/api/rh/certificados/solicitar', [
            'tipoCertificado' => 'COMPLEMENTAR', 'horasSolicitadas' => 3,
        ], $headersMembro)->assertCreated();

        $this->putJson('/api/rh/certificados/solicitacoes/'.$a->json('idSolicitacao').'/aprovar', [], $headersAdmin)
            ->assertOk();

        // A consolida tudo: B não tem mais lastro (400) e A já decidida (400).
        $this->putJson('/api/rh/certificados/solicitacoes/'.$b->json('idSolicitacao').'/aprovar', [], $headersAdmin)
            ->assertStatus(400);
        $this->putJson('/api/rh/certificados/solicitacoes/'.$a->json('idSolicitacao').'/rejeitar', [], $headersAdmin)
            ->assertStatus(400);

        // Admin sem vínculo não decide (sem id_admin_aprovador possível).
        $adminSolto = $this->criarAdmin();
        $this->putJson(
            '/api/rh/certificados/solicitacoes/'.$b->json('idSolicitacao').'/rejeitar',
            [],
            $this->authHeader($adminSolto, Role::ADMIN)
        )->assertForbidden();
    }

    public function test_solicitacoes_filtram_proprio_para_nao_admin(): void
    {
        $admin = $this->criarAdminRh();
        $a = $this->criarFuncionario(null, Role::BOLSISTA);
        $b = $this->criarFuncionario(null, Role::BOLSISTA);
        $this->comHorasValidadas((int) $a['funcionario']->getKey());
        $this->comHorasValidadas((int) $b['funcionario']->getKey());

        $this->postJson('/api/rh/certificados/solicitar', [
            'tipoCertificado' => 'EXTENSAO', 'horasSolicitadas' => 5,
        ], $this->authHeader($a['login'], Role::BOLSISTA))->assertCreated();

        $this->getJson('/api/rh/certificados/solicitacoes', $this->authHeader($a['login'], Role::BOLSISTA))
            ->assertOk()->assertJsonCount(1);
        $this->getJson('/api/rh/certificados/solicitacoes', $this->authHeader($b['login'], Role::BOLSISTA))
            ->assertOk()->assertJsonCount(0);
        $this->getJson('/api/rh/certificados/solicitacoes', $this->authHeader($admin['login'], Role::ADMIN))
            ->assertOk()->assertJsonCount(1);
    }
}
