package com.fablab.rh.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fablab.rh.dto.CertificadoAprovadoEvent;
import com.fablab.rh.dto.CertificadoRejeitadoEvent;
import com.fablab.rh.dto.CertificadoSolicitadoEvent;
import com.fablab.rh.dto.DecisaoSolicitacaoRequest;
import com.fablab.rh.dto.RhPrincipal;
import com.fablab.rh.dto.SolicitarCertificadoRequest;
import com.fablab.rh.entity.ApontamentoHoras;
import com.fablab.rh.entity.CertificadoEmitido;
import com.fablab.rh.entity.Funcionario;
import com.fablab.rh.entity.NivelAcesso;
import com.fablab.rh.entity.Pessoa;
import com.fablab.rh.entity.PessoaStatus;
import com.fablab.rh.entity.SolicitacaoCertificado;
import com.fablab.rh.entity.StatusApontamento;
import com.fablab.rh.entity.StatusSolicitacao;
import com.fablab.rh.entity.TipoApontamento;
import com.fablab.rh.entity.TipoCertificado;
import com.fablab.rh.exception.ForbiddenException;
import com.fablab.rh.exception.ResourceNotFoundException;
import com.fablab.rh.repository.ApontamentoHorasRepository;
import com.fablab.rh.repository.CertificadoEmitidoRepository;
import com.fablab.rh.repository.FuncionarioRepository;
import com.fablab.rh.repository.HoraConsolidadaRepository;
import com.fablab.rh.repository.SolicitacaoCertificadoRepository;
import com.fablab.rh.service.CertificadoService;
import com.fablab.rh.service.RhEventPublisher;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CertificadoServiceTest {

    @Mock
    private SolicitacaoCertificadoRepository solicitacaoRepository;
    @Mock
    private CertificadoEmitidoRepository certificadoRepository;
    @Mock
    private HoraConsolidadaRepository horaConsolidadaRepository;
    @Mock
    private ApontamentoHorasRepository apontamentoRepository;
    @Mock
    private FuncionarioRepository funcionarioRepository;
    @Mock
    private RhEventPublisher eventPublisher;

    @InjectMocks
    private CertificadoService certificadoService;

    private final LocalDate data = LocalDate.of(2026, 1, 5);

    private Funcionario funcionario(long id, long idPessoa, NivelAcesso nivel) {
        Pessoa pessoa = new Pessoa();
        pessoa.setId(idPessoa);
        pessoa.setNomeCompleto("Maria");
        pessoa.setMatricula("M" + idPessoa);
        pessoa.setStatus(PessoaStatus.ATIVO);
        Funcionario funcionario = new Funcionario();
        funcionario.setId(id);
        funcionario.setPessoa(pessoa);
        funcionario.setNivelAcesso(nivel);
        return funcionario;
    }

    private RhPrincipal principal(long idFuncionario, NivelAcesso nivel) {
        return new RhPrincipal(idFuncionario, idFuncionario, nivel, "FabLab");
    }

    private SolicitacaoCertificado solicitacaoPendente(long id, Funcionario funcionario) {
        SolicitacaoCertificado solicitacao = new SolicitacaoCertificado();
        solicitacao.setIdSolicitacao(id);
        solicitacao.setFuncionario(funcionario);
        solicitacao.setTipoCertificado(TipoCertificado.COMPLEMENTAR);
        solicitacao.setDataSolicitacao(LocalDateTime.now());
        solicitacao.setHorasSolicitadas(new BigDecimal("10.00"));
        solicitacao.setStatus(StatusSolicitacao.PENDENTE);
        return solicitacao;
    }

    private ApontamentoHoras apontamentoValido(long id, Funcionario funcionario, BigDecimal horas) {
        ApontamentoHoras apontamento = new ApontamentoHoras();
        apontamento.setId(id);
        apontamento.setFuncionario(funcionario);
        apontamento.setTipo(TipoApontamento.PROJETO);
        apontamento.setIdReferencia(3L);
        apontamento.setData(data);
        apontamento.setHorasTrabalhadas(horas);
        apontamento.setStatus(StatusApontamento.VALIDADO);
        apontamento.setConsolidado(false);
        return apontamento;
    }

    @Test
    void solicitarCriaSolicitacaoPendenteEPublicaEvento() {
        Funcionario funcionario = funcionario(7, 7, NivelAcesso.BOLSISTA);
        when(funcionarioRepository.findById(7L)).thenReturn(Optional.of(funcionario));
        when(apontamentoRepository.sumHorasDisponiveis(7L)).thenReturn(new BigDecimal("20.00"));
        when(solicitacaoRepository.save(any(SolicitacaoCertificado.class))).thenAnswer(inv -> {
            SolicitacaoCertificado s = inv.getArgument(0);
            s.setIdSolicitacao(11L);
            return s;
        });

        var response = certificadoService.solicitar(
                new SolicitarCertificadoRequest(TipoCertificado.ESTAGIO, new BigDecimal("10.00")),
                principal(7L, NivelAcesso.BOLSISTA));

        assertThat(response.idSolicitacao()).isEqualTo(11L);
        assertThat(response.status()).isEqualTo(StatusSolicitacao.PENDENTE);

        ArgumentCaptor<CertificadoSolicitadoEvent> captor =
                ArgumentCaptor.forClass(CertificadoSolicitadoEvent.class);
        verify(eventPublisher).publishCertificadoSolicitado(captor.capture());
        assertThat(captor.getValue().idFuncionario()).isEqualTo(7L);
        assertThat(captor.getValue().horasSolicitadas()).isEqualByComparingTo("10.00");
    }

    @Test
    void solicitarExcedeHorasDisponiveis() {
        Funcionario funcionario = funcionario(7, 7, NivelAcesso.BOLSISTA);
        when(funcionarioRepository.findById(7L)).thenReturn(Optional.of(funcionario));
        when(apontamentoRepository.sumHorasDisponiveis(7L)).thenReturn(new BigDecimal("5.00"));

        assertThatThrownBy(() -> certificadoService.solicitar(
                new SolicitarCertificadoRequest(TipoCertificado.ESTAGIO, new BigDecimal("10.00")),
                principal(7L, NivelAcesso.BOLSISTA)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("excedem");

        verify(solicitacaoRepository, never()).save(any());
        verify(eventPublisher, never()).publishCertificadoSolicitado(any());
    }

    @Test
    void solicitarSemFuncionarioVinculado() {
        assertThatThrownBy(() -> certificadoService.solicitar(
                new SolicitarCertificadoRequest(TipoCertificado.ESTAGIO, new BigDecimal("10.00")),
                new RhPrincipal(1L, null, NivelAcesso.BOLSISTA, "FabLab")))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void funcionarioVeApenasSuasSolicitacoes() {
        Funcionario funcionario = funcionario(7, 7, NivelAcesso.BOLSISTA);
        when(solicitacaoRepository.findByFuncionarioIdOrderByDataSolicitacaoDesc(7L))
                .thenReturn(List.of(solicitacaoPendente(5L, funcionario)));

        var responses = certificadoService.listarSolicitacoes(null, principal(7L, NivelAcesso.BOLSISTA));

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).idFuncionario()).isEqualTo(7L);
    }

    @Test
    void adminVeTodasAsSolicitacoes() {
        Funcionario funcionario = funcionario(7, 7, NivelAcesso.ADMIN);
        when(solicitacaoRepository.findAllByOrderByDataSolicitacaoDesc())
                .thenReturn(List.of(solicitacaoPendente(5L, funcionario)));

        var responses = certificadoService.listarSolicitacoes(null, principal(1L, NivelAcesso.ADMIN));

        assertThat(responses).hasSize(1);
        verify(solicitacaoRepository).findAllByOrderByDataSolicitacaoDesc();
    }

    @Test
    void aprovarConsolidaHorasValidasEPublicaEvento() {
        Funcionario funcionario = funcionario(7, 7, NivelAcesso.BOLSISTA);
        Funcionario admin = funcionario(1, 1, NivelAcesso.ADMIN);
        SolicitacaoCertificado solicitacao = solicitacaoPendente(11L, funcionario);

        when(solicitacaoRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(solicitacao));
        when(funcionarioRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(apontamentoRepository.sumHorasDisponiveis(7L)).thenReturn(new BigDecimal("12.00"));
        when(apontamentoRepository.findByFuncionarioIdAndStatusAndConsolidadoFalse(7L, StatusApontamento.VALIDADO))
                .thenReturn(List.of(
                        apontamentoValido(21L, funcionario, new BigDecimal("6.00")),
                        apontamentoValido(22L, funcionario, new BigDecimal("6.00"))));
        when(certificadoRepository.save(any(CertificadoEmitido.class))).thenAnswer(inv -> {
            CertificadoEmitido c = inv.getArgument(0);
            c.setIdCertificado(31L);
            c.setCodigoVerificacao(UUID.randomUUID());
            return c;
        });
        when(apontamentoRepository.save(any(ApontamentoHoras.class))).thenAnswer(inv -> inv.getArgument(0));
        when(horaConsolidadaRepository.save(any(com.fablab.rh.entity.HoraConsolidada.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(horaConsolidadaRepository.findByCertificadoIdCertificado(31L)).thenReturn(List.of());

        var response = certificadoService.aprovar(11L, new DecisaoSolicitacaoRequest(null),
                principal(1L, NivelAcesso.ADMIN));

        assertThat(response.idCertificado()).isEqualTo(31L);
        assertThat(response.horasCertificadas()).isEqualByComparingTo("10.00");
        assertThat(solicitacao.getStatus()).isEqualTo(StatusSolicitacao.APROVADO);

        ArgumentCaptor<CertificadoAprovadoEvent> captor =
                ArgumentCaptor.forClass(CertificadoAprovadoEvent.class);
        verify(eventPublisher).publishCertificadoAprovado(captor.capture());
        assertThat(captor.getValue().idFuncionario()).isEqualTo(7L);
        assertThat(captor.getValue().nomeFuncionario()).isEqualTo("Maria");

        verify(apontamentoRepository, times(2)).save(org.mockito.ArgumentMatchers.argThat(
                a -> a.getConsolidado()));
    }

    @Test
    void aprovarSolicitacaoJaDecidida() {
        Funcionario funcionario = funcionario(7, 7, NivelAcesso.BOLSISTA);
        SolicitacaoCertificado solicitacao = solicitacaoPendente(11L, funcionario);
        solicitacao.setStatus(StatusSolicitacao.APROVADO);

        when(solicitacaoRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(solicitacao));

        assertThatThrownBy(() -> certificadoService.aprovar(11L, new DecisaoSolicitacaoRequest(null),
                principal(1L, NivelAcesso.ADMIN)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("já decidida");

        verify(eventPublisher, never()).publishCertificadoAprovado(any());
    }

    @Test
    void aprovarSemHorasDisponiveisSuficientes() {
        Funcionario funcionario = funcionario(7, 7, NivelAcesso.BOLSISTA);
        Funcionario admin = funcionario(1, 1, NivelAcesso.ADMIN);
        SolicitacaoCertificado solicitacao = solicitacaoPendente(11L, funcionario);

        when(solicitacaoRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(solicitacao));
        when(funcionarioRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(apontamentoRepository.sumHorasDisponiveis(7L)).thenReturn(new BigDecimal("3.00"));

        assertThatThrownBy(() -> certificadoService.aprovar(11L, new DecisaoSolicitacaoRequest(null),
                principal(1L, NivelAcesso.ADMIN)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("insuficientes");
    }

    @Test
    void rejeitarAtualizaStatusEPublicaEvento() {
        Funcionario funcionario = funcionario(7, 7, NivelAcesso.BOLSISTA);
        Funcionario admin = funcionario(1, 1, NivelAcesso.ADMIN);
        SolicitacaoCertificado solicitacao = solicitacaoPendente(11L, funcionario);

        when(solicitacaoRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(solicitacao));
        when(funcionarioRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(solicitacaoRepository.save(any(SolicitacaoCertificado.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = certificadoService.rejeitar(11L, new DecisaoSolicitacaoRequest("Dados incompletos"),
                principal(1L, NivelAcesso.ADMIN));

        assertThat(response.status()).isEqualTo(StatusSolicitacao.REJEITADO);
        assertThat(response.observacao()).isEqualTo("Dados incompletos");

        ArgumentCaptor<CertificadoRejeitadoEvent> captor =
                ArgumentCaptor.forClass(CertificadoRejeitadoEvent.class);
        verify(eventPublisher).publishCertificadoRejeitado(captor.capture());
        assertThat(captor.getValue().observacao()).isEqualTo("Dados incompletos");
    }

    @Test
    void funcionarioNaoPodeConsultarCertificadoDeOutro() {
        Funcionario funcionario = funcionario(7, 7, NivelAcesso.BOLSISTA);
        CertificadoEmitido certificado = new CertificadoEmitido();
        certificado.setIdCertificado(31L);
        certificado.setSolicitacao(solicitacaoPendente(11L, funcionario(8, 8, NivelAcesso.VOLUNTARIO)));
        certificado.setFuncionario(funcionario(8, 8, NivelAcesso.VOLUNTARIO));
        certificado.setTipoCertificado(TipoCertificado.COMPLEMENTAR);
        certificado.setHorasCertificadas(new BigDecimal("10.00"));
        certificado.setDataEmissao(LocalDateTime.now());
        certificado.setCodigoVerificacao(UUID.randomUUID());

        when(certificadoRepository.findById(31L)).thenReturn(Optional.of(certificado));

        assertThatThrownBy(() -> certificadoService.obterEmitido(31L, principal(7L, NivelAcesso.BOLSISTA)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void obterEmitidoLancaNotFound() {
        when(certificadoRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> certificadoService.obterEmitido(999L, principal(1L, NivelAcesso.ADMIN)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void horasDisponiveisRetornaTotalParaUsuarioAutenticado() {
        when(apontamentoRepository.sumHorasDisponiveis(7L)).thenReturn(new BigDecimal("15.50"));

        var response = certificadoService.horasDisponiveis(principal(7L, NivelAcesso.BOLSISTA));

        assertThat(response.idFuncionario()).isEqualTo(7L);
        assertThat(response.totalHorasDisponiveis()).isEqualByComparingTo("15.50");
    }
}