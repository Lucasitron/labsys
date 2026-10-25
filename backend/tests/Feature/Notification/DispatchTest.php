<?php

namespace Tests\Feature\Notification;

use App\Modules\Auth\Contracts\AuthContract;
use App\Modules\Estoque\Events\CompraSolicitadaEvent as EstoqueCompra;
use App\Modules\Estoque\Events\EmprestimoAtrasadoEvent;
use App\Modules\Estoque\Events\EstoqueBaixoEvent;
use App\Modules\Financeiro\Events\CompraSolicitadaEvent as FinanceiroCompra;
use App\Modules\Financeiro\Events\LancamentoVencidoEvent;
use App\Modules\Notification\Contracts\NotificationContract;
use App\Modules\Notification\Listeners\EstoqueNotificacaoListener;
use App\Modules\Notification\Listeners\FinanceiroNotificacaoListener;
use App\Modules\Notification\Listeners\ProducaoNotificacaoListener;
use App\Modules\Notification\Listeners\RhNotificacaoListener;
use App\Modules\Notification\Listeners\VendasNotificacaoListener;
use App\Modules\Notification\Models\ConfiguracaoCanal;
use App\Modules\Notification\Models\Notificacao;
use App\Modules\Notification\Models\PreferenciaNotificacao;
use App\Modules\Producao\Events\AdvertenciaLimiteAtingidoEvent;
use App\Modules\Producao\Events\AdvertenciaRegistradaEvent;
use App\Modules\Producao\Events\KanbanStatusAlteradoEvent;
use App\Modules\Producao\Events\ProducaoConcluidaEvent;
use App\Modules\Producao\Events\ProducaoStatusAlteradoEvent;
use App\Modules\Producao\Events\ProjetoMesaAbandonadoEvent;
use App\Modules\Rh\Contracts\RhContract;
use App\Modules\Rh\Enums\NivelAcesso;
use App\Modules\Rh\Enums\TipoApontamento;
use App\Modules\Rh\Enums\TipoCertificado;
use App\Modules\Rh\Events\CertificadoAprovadoEvent;
use App\Modules\Rh\Events\CertificadoRejeitadoEvent;
use App\Modules\Rh\Events\CertificadoSolicitadoEvent;
use App\Modules\Rh\Events\ExtratoMensalHorasEvent;
use App\Modules\Rh\Events\HorasValidadasEvent;
use App\Modules\Rh\Events\NivelAlteradoEvent;
use App\Modules\Vendas\Events\EncomendaCriadaEvent;
use App\Modules\Vendas\Events\EncomendaStatusAlteradoEvent;
use App\Modules\Vendas\Events\OrcamentoAprovadoEvent;
use Carbon\CarbonImmutable;
use Illuminate\Support\Facades\Event;
use Illuminate\Support\Facades\Mail;

/** Fan-in: os 18 nomes (19 binds) geram rows c/ tipo/destinatário certos. */
class DispatchTest extends NotificationTestCase
{
    public function test_rh_seis_eventos_destinatario_e_tipo(): void
    {
        Mail::fake();
        $func = $this->funcionarioComEmail();
        $admin = $this->criarAdminRh();
        $idFunc = (int) $func['funcionario']->getKey();
        $idUser = $func['login']->id_user;
        $agora = CarbonImmutable::now();
        $ouvinte = app(RhNotificacaoListener::class);

        $ouvinte->handleNivelAlterado(new NivelAlteradoEvent($idFunc, NivelAcesso::BOLSISTA, NivelAcesso::ADMIN, $agora));
        $ouvinte->handleHorasValidadas(new HorasValidadasEvent($idFunc, TipoApontamento::ENCOMENDA, 5, '4.00', '2026-10-01'));
        $ouvinte->handleExtratoMensal(new ExtratoMensalHorasEvent($idFunc, 'Nome', '2026-09', '10.00', '4.00', '2.00', '4.00'));
        $ouvinte->handleCertificadoSolicitado(new CertificadoSolicitadoEvent(9, $idFunc, TipoCertificado::EXTENSAO, '8.00', $agora));
        $ouvinte->handleCertificadoAprovado(new CertificadoAprovadoEvent(9, 3, $idFunc, 'Nome', TipoCertificado::EXTENSAO, '8.00', $agora));
        $ouvinte->handleCertificadoRejeitado(new CertificadoRejeitadoEvent(10, $idFunc, 'Nome', TipoCertificado::ESTAGIO, 'Faltou doc', $agora));

        // 5 p/ o funcionário (tipo pessoas) + 1 broadcast p/ admins (1 row por
        // canal: inapp sempre, push/email sob prefs+config+e-mail resolvido).
        $this->assertSame(5, Notificacao::where('id_usuario', $idUser)->where('tipo', 'pessoas')->where('canal', 'inapp')->count());
        $this->assertSame(1, Notificacao::where('tipo', 'pessoas')->where('id_usuario', '!=', $idUser)->where('canal', 'inapp')->count());

        // Funcionario vinculados têm e-mail → 1 row email por evento (Mail::fake).
        $this->assertSame(5, Notificacao::where('id_usuario', $idUser)->where('canal', 'email')->count());
    }

    public function test_estoque_tres_eventos_e_compra_dupla_por_origem(): void
    {
        Mail::fake();
        $admin = $this->criarAdminRh();
        $idAdmin = $admin['login']->id_user;
        $alvo = $this->funcionarioComEmail();
        $ouvinte = app(EstoqueNotificacaoListener::class);

        $ouvinte->handleEstoqueBaixo(new EstoqueBaixoEvent(4, 'Filamento', '2.00', '10.00'));
        $ouvinte->handleEmprestimoAtrasado(new EmprestimoAtrasadoEvent(7, $alvo['login']->id_user, 4, '2026-09-30'));
        $ouvinte->handleCompraSolicitada(new EstoqueCompra(11, 4, 2, '5.00', '10.00', '50.00', '2026-10-01', null));
        app(FinanceiroNotificacaoListener::class)->handleCompraSolicitada(new FinanceiroCompra(12, null));

        $this->assertTrue(Notificacao::where('id_usuario', $idAdmin)->where('tipo', 'estoque')->exists());
        $this->assertTrue(Notificacao::where('id_usuario', $alvo['login']->id_user)->where('tipo', 'estoque')->exists());
        $this->assertTrue(Notificacao::where('id_usuario', $idAdmin)->where('tipo', 'financeiro')->exists());
    }

    public function test_vendas_tres_eventos_tipo_encomenda_por_origem(): void
    {
        Mail::fake();
        $admin = $this->criarAdminRh();
        $idAdmin = $admin['login']->id_user;
        $ouvinte = app(VendasNotificacaoListener::class);

        $ouvinte->handleEncomendaCriada(new EncomendaCriadaEvent(21, 3, '100.00', '2026-10-01'));
        $ouvinte->handleEncomendaStatusAlterado(new EncomendaStatusAlteradoEvent(21, 'Fila', 'Produção', '2026-10-02'));
        $ouvinte->handleOrcamentoAprovado(new OrcamentoAprovadoEvent(8, 3, '250.00'));

        $this->assertSame(3, Notificacao::where('id_usuario', $idAdmin)->where('tipo', 'encomenda')->where('canal', 'inapp')->count());
    }

    public function test_financeiro_vencido_so_admins(): void
    {
        Mail::fake();
        $admin = $this->criarAdminRh();
        $alheio = $this->funcionarioComEmail();

        app(FinanceiroNotificacaoListener::class)->handleLancamentoVencido(
            new LancamentoVencidoEvent(31, '99.90', '2026-09-01', null)
        );

        $this->assertTrue(Notificacao::where('id_usuario', $admin['login']->id_user)->where('tipo', 'financeiro')->exists());
        $this->assertSame(0, Notificacao::where('id_usuario', $alheio['login']->id_user)->count());
    }

    public function test_producao_seis_eventos_funcionario_admins_e_idusuario(): void
    {
        Mail::fake();
        $admin = $this->criarAdminRh();
        $idAdmin = $admin['login']->id_user;
        $func = $this->funcionarioComEmail();
        $idFunc = (int) $func['funcionario']->getKey();
        $idUser = $func['login']->id_user;
        $ouvinte = app(ProducaoNotificacaoListener::class);

        $ouvinte->handleKanbanStatusAlterado(new KanbanStatusAlteradoEvent(41, 'FILA', 'PRODUCAO', '2026-10-02'));
        $ouvinte->handleProducaoStatusAlterado(new ProducaoStatusAlteradoEvent(41, 'PRONTO', $idUser, null));
        $ouvinte->handleProducaoStatusAlterado(new ProducaoStatusAlteradoEvent(42, 'PRONTO', null, null));
        $ouvinte->handleProducaoConcluida(new ProducaoConcluidaEvent(41, null, [], '2026-10-03'));
        $ouvinte->handleAdvertenciaRegistrada(new AdvertenciaRegistradaEvent($idFunc, 1, 'Atraso'));
        $ouvinte->handleAdvertenciaLimiteAtingido(new AdvertenciaLimiteAtingidoEvent($idFunc, 3, 'Faltas'));
        $ouvinte->handleProjetoMesaAbandonado(new ProjetoMesaAbandonadoEvent(51, $idFunc, null));

        // Diretas ao usuário (idUsuario) + ao funcionário + broadcasts.
        $this->assertTrue(Notificacao::where('id_usuario', $idUser)->where('tipo', 'producao')->exists());
        $this->assertTrue(Notificacao::where('id_usuario', $idAdmin)->where('tipo', 'producao')->exists());
        $this->assertNull(Notificacao::where('link', '<>', null)->first());
    }

    public function test_evento_via_event_facade_chega_ao_listener(): void
    {
        Mail::fake();
        $func = $this->funcionarioComEmail();
        $idFunc = (int) $func['funcionario']->getKey();

        event(new NivelAlteradoEvent($idFunc, NivelAcesso::BOLSISTA, NivelAcesso::VOLUNTARIO, CarbonImmutable::now()));

        $this->assertTrue(
            Notificacao::where('id_usuario', $func['login']->id_user)->where('tipo', 'pessoas')->exists()
        );
    }

    public function test_falha_de_smtp_mantem_pendente_e_config_email_off_so_inapp(): void
    {
        $func = $this->funcionarioComEmail();
        $idFunc = (int) $func['funcionario']->getKey();

        // SMTP inalcançável → fail-soft PENDENTE (sem Mail::fake).
        config()->set('mail.default', 'smtp');
        config()->set('mail.mailers.smtp.host', '127.0.0.1');
        config()->set('mail.mailers.smtp.port', 9);
        config()->set('mail.mailers.smtp.timeout', 1);

        app(RhNotificacaoListener::class)->handleHorasValidadas(
            new HorasValidadasEvent($idFunc, TipoApontamento::PROJETO, 6, '2.00', '2026-10-02')
        );

        $pendente = Notificacao::where('id_usuario', $func['login']->id_user)->where('canal', 'email')->first();
        $this->assertNotNull($pendente);
        $this->assertSame('PENDENTE', $pendente->status->value);
        $this->assertTrue(Notificacao::where('id_usuario', $func['login']->id_user)->where('canal', 'inapp')->exists());

        // EMAIL off → zero row email (só inapp/push).
        Mail::fake();
        ConfiguracaoCanal::where('canal', 'EMAIL')->update(['habilitado' => false]);
        app(RhNotificacaoListener::class)->handleHorasValidadas(
            new HorasValidadasEvent($idFunc, TipoApontamento::PROJETO, 6, '2.00', '2026-10-02')
        );
        $this->assertSame(1, Notificacao::where('id_usuario', $func['login']->id_user)->where('canal', 'email')->count());

        // Pref-email off → zero row email nova.
        ConfiguracaoCanal::where('canal', 'EMAIL')->update(['habilitado' => true]);
        PreferenciaNotificacao::where('tipo', 'pessoas')->where('canal', 'email')->update(['habilitado' => false]);
        app(RhNotificacaoListener::class)->handleHorasValidadas(
            new HorasValidadasEvent($idFunc, TipoApontamento::PROJETO, 6, '2.00', '2026-10-02')
        );
        $this->assertSame(1, Notificacao::where('id_usuario', $func['login']->id_user)->where('canal', 'email')->count());

        // Sem e-mail do destinatário → sem row email, sem 500.
        $semEmail = $this->funcionarioComEmail(\App\Modules\Auth\Enums\Role::BOLSISTA, 'sem-email');
        app(RhNotificacaoListener::class)->handleHorasValidadas(
            new HorasValidadasEvent((int) $semEmail['funcionario']->getKey(), TipoApontamento::PROJETO, 6, '2.00', '2026-10-02')
        );
        $this->assertSame(0, Notificacao::where('id_usuario', $semEmail['login']->id_user)->where('canal', 'email')->count());
        $this->assertTrue(Notificacao::where('id_usuario', $semEmail['login']->id_user)->where('canal', 'inapp')->exists());
    }

    public function test_funcionario_inexistente_warn_sem_500_e_contracts(): void
    {
        Mail::fake();
        app(RhNotificacaoListener::class)->handleHorasValidadas(
            new HorasValidadasEvent(999999, TipoApontamento::PROJETO, 6, '2.00', '2026-10-02')
        );
        $this->assertSame(0, Notificacao::count());

        // Broadcast sem Admin = warn + zero rows, nunca 500.
        app(VendasNotificacaoListener::class)->handleEncomendaCriada(new EncomendaCriadaEvent(99, 1, '10.00', '2026-10-01'));
        $this->assertSame(0, Notificacao::count());

        // Contracts aditivos (precedente M3/E9 — sem quebra).
        $func = $this->funcionarioComEmail();
        $destino = app(RhContract::class)->dadosDestinatario((int) $func['funcionario']->getKey());
        $this->assertSame($func['login']->id_user, $destino['idUsuario']);
        $this->assertStringContainsString('@', (string) $destino['email']);
        $this->assertNull(app(RhContract::class)->dadosDestinatario(999999));

        $admin = $this->criarAdminRh();
        $this->assertContains($admin['login']->id_user, app(AuthContract::class)->adminIds());

        $contract = app(NotificationContract::class);
        $this->assertSame(0, $contract->naoLidas($func['login']->id_user));
        $this->assertSame(0, $contract->inbox($func['login']->id_user, 1, 10)['total']);
    }

    public function test_producao_status_sem_idusuario_vai_p_admins(): void
    {
        Event::fake();
        $admin = $this->criarAdminRh();

        app(ProducaoNotificacaoListener::class)->handleProducaoStatusAlterado(
            new ProducaoStatusAlteradoEvent(77, 'ENTREGUE', null, null)
        );

        $this->assertTrue(Notificacao::where('id_usuario', $admin['login']->id_user)->where('tipo', 'producao')->exists());
    }
}
