<?php

namespace Tests\Feature\Producao;

use App\Modules\Auth\Enums\Role;
use App\Modules\Producao\Events\AdvertenciaLimiteAtingidoEvent;
use App\Modules\Producao\Events\AdvertenciaRegistradaEvent;
use App\Modules\Producao\Models\Parametro5S;
use Illuminate\Support\Facades\Event;

/** Inspeção 5S + advertências: status server-side, contador, limite e experimental. */
class InspecaoAdvertenciaTest extends ProducaoTestCase
{
    private function setorComChecklist(array $headers, int $responsavel): array
    {
        $setorId = $this->postJson('/api/producao/setores', ['numero' => 1, 'nome' => 'Mecânica'], $headers)
            ->assertCreated()->json('id');
        $checkId = $this->postJson("/api/producao/setores/{$setorId}/checklist", ['item' => 'Piso limpo'], $headers)
            ->assertCreated()->json('id');
        $this->postJson("/api/producao/setores/{$setorId}/responsaveis", [
            'idFuncionario' => $responsavel, 'dataInicio' => today()->toDateString(),
        ], $headers)->assertCreated();

        return [$setorId, $checkId];
    }

    private function inspecaoPayload(int $setorId, int $inspetor, int $checkId, bool $conforme): array
    {
        return [
            'idSetor' => $setorId, 'idInspetor' => $inspetor,
            'dataInspecao' => today()->toDateString(), 'turno' => 'MANHA',
            'itens' => [['idChecklist' => $checkId, 'conforme' => $conforme]],
        ];
    }

    public function test_inspecao_conforme_ok_e_item_estranho_422(): void
    {
        $admin = $this->headersAdmin();
        [$setorId, $checkId] = $this->setorComChecklist($admin, 61);

        // Payload com status divergente é ignorado (sem campo status no contrato).
        $this->postJson('/api/producao/inspecoes-5s',
            $this->inspecaoPayload($setorId, 62, $checkId, true) + ['status' => 'NAO_CONFORME'], $admin)
            ->assertCreated()->assertJsonPath('status', 'OK');

        $outroSetor = $this->postJson('/api/producao/setores', ['numero' => 2, 'nome' => 'Outro'], $admin)
            ->assertCreated()->json('id');
        $this->postJson('/api/producao/inspecoes-5s',
            $this->inspecaoPayload($outroSetor, 62, $checkId, true), $admin)->assertStatus(422);

        $this->getJson('/api/producao/inspecoes-5s', $admin)->assertOk()->assertJsonCount(1);
        $this->getJson("/api/producao/inspecoes-5s?idSetor={$setorId}", $admin)->assertOk()->assertJsonCount(1);
    }

    public function test_nao_conformidade_gera_auto_advertencia_verbal(): void
    {
        Event::fake([AdvertenciaRegistradaEvent::class]);
        $admin = $this->headersAdmin();
        [$setorId, $checkId] = $this->setorComChecklist($admin, 61);

        $this->postJson('/api/producao/inspecoes-5s',
            $this->inspecaoPayload($setorId, 62, $checkId, false), $admin)
            ->assertCreated()->assertJsonPath('status', 'NAO_CONFORME');

        Event::assertDispatched(AdvertenciaRegistradaEvent::class,
            fn ($e) => $e->idFuncionario === 61 && $e->contador === 1);

        $this->getJson('/api/producao/advertencias/61', $admin)->assertOk()->assertJsonCount(1);
    }

    public function test_advertencia_manual_admin_limite_3_publica_2_eventos(): void
    {
        Event::fake([AdvertenciaRegistradaEvent::class, AdvertenciaLimiteAtingidoEvent::class]);
        $admin = $this->headersAdmin();

        foreach ([1, 2] as $n) {
            $this->postJson('/api/producao/advertencias', [
                'idFuncionario' => 63, 'motivo' => "Falta {$n}", 'tipo' => 'FORMAL',
            ], $admin)->assertCreated()->assertJsonPath('contador', $n);
        }
        Event::assertDispatched(AdvertenciaRegistradaEvent::class, fn ($e) => $e->contador === 2);
        Event::assertNotDispatched(AdvertenciaLimiteAtingidoEvent::class);

        $this->postJson('/api/producao/advertencias', [
            'idFuncionario' => 63, 'motivo' => 'Falta 3', 'tipo' => 'FORMAL',
        ], $admin)->assertCreated()->assertJsonPath('contador', 3);
        Event::assertDispatched(AdvertenciaLimiteAtingidoEvent::class,
            fn ($e) => $e->idFuncionario === 63 && $e->contador === 3);

        // Não-Admin registra → 403; não-Admin lista só as próprias.
        $bolsista = $this->loginPapel(Role::BOLSISTA);
        $headersB = $this->authHeader($bolsista, Role::BOLSISTA);
        $this->postJson('/api/producao/advertencias', [
            'idFuncionario' => 63, 'motivo' => 'X', 'tipo' => 'VERBAL',
        ], $headersB)->assertForbidden();
        $this->getJson('/api/producao/advertencias', $headersB)->assertOk()->assertJsonCount(0);
        $this->getJson("/api/producao/advertencias/{$bolsista->id_user}", $headersB)->assertOk()->assertJsonCount(0);
    }

    public function test_experimental_on_nao_conta_para_limite(): void
    {
        Event::fake([AdvertenciaRegistradaEvent::class, AdvertenciaLimiteAtingidoEvent::class]);
        $this->assertTrue(Parametro5S::experimentalAtivo());

        $admin = $this->headersAdmin();
        [$setorId, $checkId] = $this->setorComChecklist($admin, 64);

        for ($i = 0; $i < 3; $i++) {
            $this->postJson('/api/producao/inspecoes-5s',
                $this->inspecaoPayload($setorId, 65, $checkId, false), $admin)->assertCreated();
        }

        Event::assertDispatched(AdvertenciaRegistradaEvent::class, fn ($e) => $e->contador === 3);
        Event::assertNotDispatched(AdvertenciaLimiteAtingidoEvent::class);

        // Com experimental off, a 4ª (manual) publica o limite.
        Parametro5S::where('chave', 'periodoExperimentalAtivo')->update(['valor' => 'false']);
        $this->postJson('/api/producao/advertencias', [
            'idFuncionario' => 64, 'motivo' => 'Formal', 'tipo' => 'FORMAL',
        ], $admin)->assertCreated()->assertJsonPath('contador', 4);
        Event::assertDispatched(AdvertenciaLimiteAtingidoEvent::class, fn ($e) => $e->contador === 4);
    }

    public function test_inspecao_delete_so_admin_e_vinculo_inspetor(): void
    {
        $admin = $this->headersAdmin();
        [$setorId, $checkId] = $this->setorComChecklist($admin, 61);

        $inspetor = $this->loginPapel(Role::BOLSISTA);
        $headersI = $this->authHeader($inspetor, Role::BOLSISTA);

        // Inspetor registra a própria; outro não registra por ele.
        $id = $this->postJson('/api/producao/inspecoes-5s',
            $this->inspecaoPayload($setorId, $inspetor->id_user, $checkId, true), $headersI)
            ->assertCreated()->json('id');
        $this->postJson('/api/producao/inspecoes-5s',
            $this->inspecaoPayload($setorId, $inspetor->id_user, $checkId, true),
            $this->headersPapel(Role::BOLSISTA))->assertForbidden();

        $this->deleteJson("/api/producao/inspecoes-5s/{$id}", [], $headersI)->assertForbidden();
        $this->deleteJson("/api/producao/inspecoes-5s/{$id}", [], $admin)->assertNoContent();
    }
}
