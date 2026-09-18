package com.fablab.rh.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fablab.rh.dto.ExtratoMensalHorasEvent;
import com.fablab.rh.entity.Funcionario;
import com.fablab.rh.entity.NivelAcesso;
import com.fablab.rh.entity.Pessoa;
import com.fablab.rh.entity.PessoaStatus;
import com.fablab.rh.entity.TipoApontamento;
import com.fablab.rh.repository.ApontamentoHorasRepository;
import com.fablab.rh.repository.FuncionarioRepository;
import com.fablab.rh.repository.RegistroPontoDiarioRepository;
import com.fablab.rh.service.ExtratoService;
import com.fablab.rh.service.RhEventPublisher;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExtratoServiceTest {

    @Mock
    private FuncionarioRepository funcionarioRepository;
    @Mock
    private RegistroPontoDiarioRepository pontoRepository;
    @Mock
    private ApontamentoHorasRepository apontamentoRepository;
    @Mock
    private RhEventPublisher eventPublisher;

    @InjectMocks
    private ExtratoService extratoService;

    private static final YearMonth REFERENCIA = YearMonth.of(2026, 1);
    private static final LocalDate INICIO = REFERENCIA.atDay(1);
    private static final LocalDate FIM = REFERENCIA.atEndOfMonth();

    private Funcionario funcionario(long id, long idPessoa, NivelAcesso nivel, PessoaStatus status) {
        Pessoa pessoa = new Pessoa();
        pessoa.setId(idPessoa);
        pessoa.setNomeCompleto("Maria");
        pessoa.setMatricula("M" + idPessoa);
        pessoa.setStatus(status);
        Funcionario funcionario = new Funcionario();
        funcionario.setId(id);
        funcionario.setPessoa(pessoa);
        funcionario.setNivelAcesso(nivel);
        return funcionario;
    }

    @Test
    void geraExtratoSobeEventoComSomasDaReferencia() {
        Funcionario ativo = funcionario(7, 7, NivelAcesso.BOLSISTA, PessoaStatus.ATIVO);
        when(funcionarioRepository.findByNivelAcessoNot(NivelAcesso.RECRUTANDO)).thenReturn(List.of(ativo));

        when(pontoRepository.sumTotalHorasPeriodo(7L, INICIO, FIM)).thenReturn(new BigDecimal("80.00"));
        when(apontamentoRepository.sumHorasValidadasPorTipoEPeriodo(7L, TipoApontamento.ENCOMENDA, INICIO, FIM))
                .thenReturn(new BigDecimal("20.00"));
        when(apontamentoRepository.sumHorasValidadasPorTipoEPeriodo(7L, TipoApontamento.PROJETO, INICIO, FIM))
                .thenReturn(new BigDecimal("30.00"));
        when(apontamentoRepository.sumHorasDisponiveis(7L)).thenReturn(new BigDecimal("120.00"));

        extratoService.gerarExtratoMensal(REFERENCIA);

        ArgumentCaptor<ExtratoMensalHorasEvent> captor =
                ArgumentCaptor.forClass(ExtratoMensalHorasEvent.class);
        verify(eventPublisher).publishExtratoMensal(captor.capture());
        ExtratoMensalHorasEvent evento = captor.getValue();

        assertThat(evento.idFuncionario()).isEqualTo(7L);
        assertThat(evento.nome()).isEqualTo("Maria");
        assertThat(evento.mesReferencia()).isEqualTo("01/2026");
        assertThat(evento.horasPresenca()).isEqualByComparingTo("80.00");
        assertThat(evento.horasEncomenda()).isEqualByComparingTo("20.00");
        assertThat(evento.horasProjeto()).isEqualByComparingTo("30.00");
        assertThat(evento.horasDisponiveis()).isEqualByComparingTo("120.00");
    }

    @Test
    void valoresNulosViramZeroNoExtrato() {
        Funcionario ativo = funcionario(7, 7, NivelAcesso.BOLSISTA, PessoaStatus.ATIVO);
        when(funcionarioRepository.findByNivelAcessoNot(NivelAcesso.RECRUTANDO)).thenReturn(List.of(ativo));
        when(pontoRepository.sumTotalHorasPeriodo(7L, INICIO, FIM)).thenReturn(null);
        when(apontamentoRepository.sumHorasValidadasPorTipoEPeriodo(7L, TipoApontamento.ENCOMENDA, INICIO, FIM))
                .thenReturn(null);
        when(apontamentoRepository.sumHorasValidadasPorTipoEPeriodo(7L, TipoApontamento.PROJETO, INICIO, FIM))
                .thenReturn(null);
        when(apontamentoRepository.sumHorasDisponiveis(7L)).thenReturn(null);

        extratoService.gerarExtratoMensal(REFERENCIA);

        ArgumentCaptor<ExtratoMensalHorasEvent> captor =
                ArgumentCaptor.forClass(ExtratoMensalHorasEvent.class);
        verify(eventPublisher).publishExtratoMensal(captor.capture());
        assertThat(captor.getValue().horasPresenca()).isEqualByComparingTo("0");
        assertThat(captor.getValue().horasEncomenda()).isEqualByComparingTo("0");
    }

    @Test
    void excluiRecrutandosEPessoasInativas() {
        Funcionario recrutando = funcionario(1, 1, NivelAcesso.RECRUTANDO, PessoaStatus.RECRUTANDO);
        Funcionario inativo = funcionario(2, 2, NivelAcesso.BOLSISTA, PessoaStatus.INATIVO);
        Funcionario ativo = funcionario(3, 3, NivelAcesso.VOLUNTARIO, PessoaStatus.ATIVO);

        // findByNivelAcessoNot(RECRUTANDO) exclui o recrutando; o inativo é filtrado pelo status.
        when(funcionarioRepository.findByNivelAcessoNot(NivelAcesso.RECRUTANDO)).thenReturn(List.of(inativo, ativo));
        when(pontoRepository.sumTotalHorasPeriodo(3L, INICIO, FIM)).thenReturn(BigDecimal.ZERO);
        when(apontamentoRepository.sumHorasValidadasPorTipoEPeriodo(3L, TipoApontamento.ENCOMENDA, INICIO, FIM))
                .thenReturn(BigDecimal.ZERO);
        when(apontamentoRepository.sumHorasValidadasPorTipoEPeriodo(3L, TipoApontamento.PROJETO, INICIO, FIM))
                .thenReturn(BigDecimal.ZERO);
        when(apontamentoRepository.sumHorasDisponiveis(3L)).thenReturn(BigDecimal.ZERO);

        extratoService.gerarExtratoMensal(REFERENCIA);

        verify(eventPublisher, times(1)).publishExtratoMensal(org.mockito.ArgumentMatchers.argThat(
                e -> e.idFuncionario().equals(3L)));
    }

    @Test
    void semFuncionariosAtivosNaoPublicaEvento() {
        when(funcionarioRepository.findByNivelAcessoNot(NivelAcesso.RECRUTANDO)).thenReturn(List.of());

        extratoService.gerarExtratoMensal(REFERENCIA);

        verify(eventPublisher, never()).publishExtratoMensal(org.mockito.ArgumentMatchers.any());
    }
}