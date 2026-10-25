<?php

namespace Tests\Unit\Rh;

use App\Modules\Auth\Events\RfidAccessEvent;
use App\Modules\Rh\Enums\NivelAcesso;
use App\Modules\Rh\Enums\PessoaStatus;
use App\Modules\Rh\Enums\StatusApontamento;
use App\Modules\Rh\Enums\TipoApontamento;
use App\Modules\Rh\Enums\TipoCertificado;
use App\Modules\Rh\Events\CertificadoAprovadoEvent;
use App\Modules\Rh\Events\CertificadoRejeitadoEvent;
use App\Modules\Rh\Events\CertificadoSolicitadoEvent;
use App\Modules\Rh\Events\ExtratoMensalHorasEvent;
use App\Modules\Rh\Events\HorasValidadasEvent;
use App\Modules\Rh\Events\NivelAlteradoEvent;
use Carbon\CarbonImmutable;
use PHPUnit\Framework\TestCase;

/** Contrato dos eventos: nomes preservados + shape do payload. */
class RhEventsTest extends TestCase
{
    public function test_nomes_preservados(): void
    {
        $this->assertSame('access.rfid.event', RfidAccessEvent::NAME);
        $this->assertSame('nivel.alterado.event', NivelAlteradoEvent::NAME);
        $this->assertSame('horas.validadas.event', HorasValidadasEvent::NAME);
        $this->assertSame('certificado.solicitado.event', CertificadoSolicitadoEvent::NAME);
        $this->assertSame('certificado.aprovado.event', CertificadoAprovadoEvent::NAME);
        $this->assertSame('certificado.rejeitado.event', CertificadoRejeitadoEvent::NAME);
        $this->assertSame('extrato.mensal.horas.event', ExtratoMensalHorasEvent::NAME);
    }

    public function test_payloads(): void
    {
        $agora = CarbonImmutable::parse('2026-08-04 08:00:00');

        $this->assertSame(
            ['idFuncionario' => 1, 'nivelAntigo' => 1, 'nivelNovo' => 2, 'data' => $agora->toIso8601String()],
            (new NivelAlteradoEvent(1, NivelAcesso::BOLSISTA, NivelAcesso::VOLUNTARIO, $agora))->payload()
        );

        $this->assertSame(
            ['idFuncionario' => 1, 'tipo' => 'ENCOMENDA', 'idReferencia' => 5, 'horas' => '3.00', 'data' => '2026-08-04'],
            (new HorasValidadasEvent(1, TipoApontamento::ENCOMENDA, 5, '3.00', '2026-08-04'))->payload()
        );

        $this->assertSame(
            ['idSolicitacao' => 1, 'idFuncionario' => 2, 'tipoCertificado' => 'EXTENSAO', 'horasSolicitadas' => '5.00', 'dataSolicitacao' => $agora->toIso8601String()],
            (new CertificadoSolicitadoEvent(1, 2, TipoCertificado::EXTENSAO, '5.00', $agora))->payload()
        );

        $this->assertSame(
            ['idSolicitacao' => 1, 'idCertificado' => 7, 'idFuncionario' => 2, 'nomeFuncionario' => 'Ada', 'tipoCertificado' => 'EXTENSAO', 'horasCertificadas' => '5.00', 'dataEmissao' => $agora->toIso8601String()],
            (new CertificadoAprovadoEvent(1, 7, 2, 'Ada', TipoCertificado::EXTENSAO, '5.00', $agora))->payload()
        );

        $this->assertSame(
            ['idSolicitacao' => 1, 'idFuncionario' => 2, 'nomeFuncionario' => 'Ada', 'tipoCertificado' => 'ESTAGIO', 'observacao' => 'x', 'dataDecisao' => $agora->toIso8601String()],
            (new CertificadoRejeitadoEvent(1, 2, 'Ada', TipoCertificado::ESTAGIO, 'x', $agora))->payload()
        );

        $this->assertSame(
            ['idFuncionario' => 2, 'nome' => 'Ada', 'mesReferencia' => '08/2026', 'horasPresenca' => '4.00', 'horasEncomenda' => '3.00', 'horasProjeto' => '0.00', 'horasDisponiveis' => '1.00'],
            (new ExtratoMensalHorasEvent(2, 'Ada', '08/2026', '4.00', '3.00', '0.00', '1.00'))->payload()
        );
    }

    public function test_parse_de_enums(): void
    {
        $this->assertSame(NivelAcesso::BOLSISTA, NivelAcesso::parse('BOLSISTA'));
        $this->assertSame(NivelAcesso::VOLUNTARIO, NivelAcesso::parse(2));
        $this->assertSame(NivelAcesso::ADMIN, NivelAcesso::parse(NivelAcesso::ADMIN));
        $this->assertSame(PessoaStatus::ATIVO, PessoaStatus::parse('ATIVO'));
        $this->assertSame(PessoaStatus::RECRUTANDO, PessoaStatus::parse(2));
        $this->assertSame(PessoaStatus::INATIVO, PessoaStatus::parse(PessoaStatus::INATIVO));
        $this->assertSame(StatusApontamento::VALIDADO, StatusApontamento::from('VALIDADO'));

        $this->expectException(\InvalidArgumentException::class);
        NivelAcesso::parse('INEXISTENTE');
    }

    public function test_parse_status_invalido_rejeita(): void
    {
        $this->expectException(\InvalidArgumentException::class);
        PessoaStatus::parse('INEXISTENTE');
    }

    public function test_labels_dos_enums(): void
    {
        $this->assertSame(
            ['Admin', 'Bolsista', 'Voluntário', 'Estagiário', 'Recrutando'],
            array_map(fn (NivelAcesso $n) => $n->label(), NivelAcesso::cases())
        );
        $this->assertSame(
            ['Ativo', 'Inativo', 'Recrutando'],
            array_map(fn (PessoaStatus $s) => $s->label(), PessoaStatus::cases())
        );
    }
}
