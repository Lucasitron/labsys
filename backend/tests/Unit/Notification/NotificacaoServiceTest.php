<?php

namespace Tests\Unit\Notification;

use App\Modules\Notification\Enums\StatusEntrega;
use App\Modules\Notification\Exceptions\LinkInvalidoException;
use App\Modules\Notification\Models\ConfiguracaoCanal;
use App\Modules\Notification\Models\Notificacao;
use App\Modules\Notification\Models\PreferenciaNotificacao;
use App\Modules\Notification\Services\NotificacaoService;
use Tests\TestCase;

/** Regras do inbox (validações 400/422/404/403, matriz default, revisão). */
class NotificacaoServiceTest extends TestCase
{
    private NotificacaoService $inbox;

    protected function setUp(): void
    {
        parent::setUp();
        $this->inbox = app(NotificacaoService::class);
    }

    public function test_matriz_default_java_tudo_true_exceto_sistema_push(): void
    {
        $prefs = $this->inbox->obterPreferencias();

        $this->assertCount(6, $prefs);
        $this->assertSame(['inapp' => true, 'email' => true, 'push' => true], $prefs['pessoas']);
        $this->assertFalse($prefs['sistema']['push']);
        $this->assertTrue($prefs['sistema']['inapp']);
        $this->assertTrue($prefs['sistema']['email']);
    }

    public function test_seed_v4_canais_e_matriz(): void
    {
        $this->assertTrue(ConfiguracaoCanal::where('canal', 'EMAIL')->first()->habilitado);
        $this->assertFalse(ConfiguracaoCanal::where('canal', 'WHATSAPP')->first()->habilitado);
        $this->assertSame(18, PreferenciaNotificacao::count());
    }

    public function test_paginacao_invalida_400(): void
    {
        try {
            $this->inbox->listar(7, 0, 10);
            $this->fail('page=0 deveria lançar');
        } catch (\InvalidArgumentException $e) {
            $this->assertStringContainsString('Página inválida', $e->getMessage());
        }

        foreach ([0, 101] as $size) {
            try {
                $this->inbox->listar(7, 1, $size);
                $this->fail("size={$size} deveria lançar");
            } catch (\InvalidArgumentException $e) {
                $this->assertStringContainsString('Tamanho de página inválido', $e->getMessage());
            }
        }
    }

    public function test_salvar_preferencias_estrita_400(): void
    {
        foreach ([
            [],
            ['x' => ['inapp' => true]],
            ['pessoas' => []],
            ['pessoas' => ['zap' => true]],
            ['pessoas' => ['inapp' => null]],
        ] as $matriz) {
            try {
                $this->inbox->salvarPreferencias($matriz);
                $this->fail('matriz inválida deveria lançar: '.json_encode($matriz));
            } catch (\InvalidArgumentException) {
                $this->assertTrue(true);
            }
        }

        $ok = $this->inbox->salvarPreferencias(['pessoas' => ['inapp' => true, 'email' => false, 'push' => true]]);
        $this->assertFalse($ok['pessoas']['email']);
        $this->assertFalse($this->inbox->preferenciaHabilitada('pessoas', 'email'));
    }

    public function test_periodo_invalido_400_e_cortes_ok(): void
    {
        try {
            $this->inbox->historico(null, null, null, null, 'ontem');
            $this->fail('periodo=ontem deveria lançar');
        } catch (\InvalidArgumentException $e) {
            $this->assertStringContainsString('Período inválido', $e->getMessage());
        }

        foreach (['24h', '30d', ' 7D ', null, ''] as $periodo) {
            $saida = $this->inbox->historico(null, null, null, null, $periodo);
            $this->assertSame(0, $saida['total']);
        }
    }

    public function test_registrar_exige_titulo_e_link_valido(): void
    {
        try {
            $this->inbox->registrar(7, 'pessoas', 'inapp', '  ', 'corpo', null);
            $this->fail('título em branco deveria lançar');
        } catch (\InvalidArgumentException) {
            $this->assertTrue(true);
        }

        try {
            $this->inbox->registrar(7, 'pessoas', 'inapp', 'T', 'corpo', 'javascript:x');
            $this->fail('link inválido deveria lançar 422');
        } catch (LinkInvalidoException) {
            $this->assertTrue(true);
        }

        $n = $this->inbox->registrar(7, 'pessoas', 'inapp', '  Título  ', 'corpo', null, 42);
        $this->assertSame('Título', $n->titulo);
        $this->assertSame(StatusEntrega::PENDENTE, $n->status);
        $this->assertSame(42, (int) $n->id_referencia);
    }

    public function test_ler_alheia_sem_admin_403_e_ausente_404(): void
    {
        $n = Notificacao::create([
            'id_usuario' => 11, 'titulo' => 'T', 'lida' => false,
            'criada_em' => now(), 'status' => StatusEntrega::ENVIADA,
        ]);

        try {
            $this->inbox->marcarComoLida((int) $n->getKey(), 22, false);
            $this->fail('leitura alheia deveria lançar');
        } catch (\App\Shared\Exceptions\ForbiddenException) {
            $this->assertTrue(true);
        }

        try {
            $this->inbox->marcarComoLida(999999, 11, false);
            $this->fail('ausente deveria lançar');
        } catch (\App\Shared\Exceptions\ResourceNotFoundException) {
            $this->assertTrue(true);
        }

        // Admin lê a de outro (bypass Java 1:1) e carimba leitura.
        $lida = $this->inbox->marcarComoLida((int) $n->getKey(), 22, true);
        $this->assertTrue((bool) $lida->lida);
        $this->assertNotNull($lida->data_leitura);
    }

    public function test_revisar_move_e_some_do_ativo_idempotente(): void
    {
        $a = Notificacao::create([
            'id_usuario' => 11, 'titulo' => 'A', 'lida' => true,
            'criada_em' => now(), 'status' => StatusEntrega::ENVIADA,
        ]);
        $b = Notificacao::create([
            'id_usuario' => 11, 'titulo' => 'B', 'lida' => false,
            'criada_em' => now(), 'status' => StatusEntrega::ENVIADA,
        ]);

        $this->assertSame(2, $this->inbox->revisar([(int) $a->getKey(), (int) $b->getKey()], 1));
        $this->assertSame(0, Notificacao::whereIn('id', [$a->getKey(), $b->getKey()])->count());

        $hist = $this->inbox->historico(null, null, null, null, null);
        $this->assertSame(2, $hist['total']);

        // Revisar de novo = idempotente (ausentes ignorados).
        $this->assertSame(0, $this->inbox->revisar([(int) $a->getKey()], 1));
    }
}
