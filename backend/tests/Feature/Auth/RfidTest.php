<?php

namespace Tests\Feature\Auth;

use App\Modules\Auth\Events\RfidAccessEvent;
use App\Modules\Auth\Models\AccessLog;
use Illuminate\Support\Facades\Event;
use Tests\TestCase;

class RfidTest extends TestCase
{
    public function test_cartao_desconhecido_registra_negado_e_retorna_allowed_false(): void
    {
        Event::fake([RfidAccessEvent::class]);

        $res = $this->postJson('/api/auth/validate-rfid', ['uuid' => 'cartao-xyz'])
            ->assertOk()
            ->assertJson(['allowed' => false, 'type' => 'ACESSO_NEGADO', 'idUser' => null])
            ->json();

        $this->assertSame('cartao-xyz', $res['uuidRfid']);

        $log = AccessLog::where('uuid_rfid', 'cartao-xyz')->first();
        $this->assertNull($log->id_user);

        Event::assertNotDispatched(RfidAccessEvent::class);
    }

    public function test_cartao_conhecido_alterna_entrada_saida_e_publica_evento(): void
    {
        Event::fake([RfidAccessEvent::class]);
        $login = $this->criarLogin(['uuid' => 'cartao-1']);

        $primeira = $this->postJson('/api/auth/validate-rfid', ['uuid' => 'cartao-1'])
            ->assertOk()
            ->assertJson(['allowed' => true, 'type' => 'ENTRADA', 'idUser' => $login->id_user])
            ->json();

        $segunda = $this->postJson('/api/auth/validate-rfid', ['uuid' => 'cartao-1'])
            ->assertOk()
            ->assertJson(['allowed' => true, 'type' => 'SAIDA'])
            ->json();

        $this->assertNotSame($primeira['type'], $segunda['type']);
        $logs = AccessLog::where('uuid_rfid', 'cartao-1')->orderBy('id')->get();
        $this->assertCount(2, $logs);
        $this->assertTrue($logs[1]->id > $logs[0]->id);

        Event::assertDispatched(RfidAccessEvent::class, 2);
        Event::assertDispatched(
            RfidAccessEvent::class,
            fn (RfidAccessEvent $e) => $e->payload()['idUser'] === $login->id_user
                && $e->payload()['uuidRfid'] === 'cartao-1'
                && $e::NAME === 'access.rfid.event'
        );
    }

    public function test_validate_rfid_e_publica_sem_token(): void
    {
        $this->postJson('/api/auth/validate-rfid', ['uuid' => 'livre'])
            ->assertOk()
            ->assertJson(['allowed' => false]);
    }

    public function test_uuid_obrigatorio(): void
    {
        $this->postJson('/api/auth/validate-rfid', [])->assertStatus(422);
    }
}
